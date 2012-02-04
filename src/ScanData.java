package lidar;

import java.util.StringTokenizer;




/**
 * Container for a given LMS111 scan.
 * 
 * Stores remission and range values for one scan of the LMS111 unit.  It also
 * stores basic information regarding the device such as the version and
 * the device status, and angle step width.
 * 
 * @author Matthew Delaney, Steven Kang
 */
public class ScanData {
	/** the raw data from LIDAR */
	private String originalData = "";
	
	/**	0x00 Device is Okay
	 *  0x01 Device Error
	 *  0x02 Contamination warning
	 *  0x04 Contamination error
	 */
	private int deviceStatus = -1;	
	/** In 1/10,000 degrees */
	private int angleStepWidth = -1;
	/** Distance in mm */
	private int[] dist1;
	/** Distance in mm */
	private int[] dist2;
	
	private int[] rssi1;
	private int[] rssi2;
	
	/**
	 * Constructs a ScanData object given an array of range values and an 
	 * array of remission values
	 * 
	 * @param dist an array of range values in mm
	 * @param remission an array of remission values in 1/10,000 degree 
	 */
	public ScanData (int[] dist1, int[] rssi1) {
		if(dist1.length != rssi1.length)
			throw new IllegalArgumentException();
		this.dist1 = dist1.clone();
		this.rssi1 = rssi1.clone();
	}
	/**
	 * Constructs a ScanData object given an output from LMS with the 
	 * "sRN LMDscandata" command.
	 * 
	 * @param scan a string of output from LMS
	 * @exception throws an IllegalArgumentException for bad input.  The output
	 * 				must include DIST1 and RSSI1 values.
	 */
	public ScanData (String scan) {
		if(!loadScan(originalData = scan)) throw new IllegalArgumentException();
	}
	
	/**
	 * Constructs a ScanData object given Strings for the known pose, distances,
	 * and remission values that haven't been parsed yet
	 * 
	 * @param pose The known pose
	 * @param dists The String of distances
	 * @param rssi The String of remission values
	 * @exception throws and IllegalArgumentException for bad input.  The output
	 * 				must include DIST1 and RSSI1 values
	 */
	public ScanData(String dist, String rssi, int angFreq){
		originalData = "";
		if (!loadScan(dist, rssi, angFreq)) throw new IllegalArgumentException();
	}
	
	/**
	 * Returns an array of ranges.
	 * @return an array of ranges
	 */
	public int[] getDIST1(){
		return dist1;
	}
	
	/**
	 * Returns an array of ranges.
	 * @return an array of ranges
	 */
	public int[] getDIST2(){
		return dist2;
	}
	
	/** 
	 * Returns an array of remission values.
	 * @return an array of remission values.
	 */
	public int[] getRSSI1(){
		return rssi1;
	}
	
	/** 
	 * Returns an array of remission values.
	 * @return an array of remission values.
	 */
	public int[] getRSSI2(){
		return rssi2;
	}
	/**
	 * Takes a return message from LMS and "loads" range and remission values.
	 * Assume only one DIST and RSSI is given
	 * 
	 * @param scan 
	 */
	public boolean loadScan(String scan){
		String[] data = scan.split(" ");
		int numData = 0;
		int index = 0;
		
		deviceStatus = Integer.parseInt(data[6]);
		
		/* DIST1 */
		if(scan.contains("DIST1")) {
			for(index = 0; index < data.length; index++)
				if(data[index].equals("DIST1")) break;
			
			angleStepWidth = Integer.parseInt(data[index+4],16);
			dist1 = new int[numData = Integer.parseInt(data[index+5],16)];
			for(int i = 0; i < numData; i++) 
				dist1[i]=Integer.parseInt(data[index+6+i],16);
		}
		
		/* DIST2 */
		if(scan.contains("DIST2")) {
			for(index = 0; index < data.length; index++)
				if(data[index].equals("DIST2")) break;
			
			angleStepWidth = Integer.parseInt(data[index+4],16);
			dist2 = new int[numData = Integer.parseInt(data[index+5],16)];
			for(int i = 0; i < numData; i++) 
				dist2[i]=Integer.parseInt(data[index+6+i],16);
		}
		
		/* RSSI1 */
		if(scan.contains("RSSI1")) {
			for(index = 0; index < data.length; index++)
				if(data[index].equals("RSSI1")) break;

			angleStepWidth = Integer.parseInt(data[index+4],16);
			rssi1 = new int[numData = Integer.parseInt(data[index+5],16)];
			for(int i = 0; i < numData; i++) 
				rssi1[i]=Integer.parseInt(data[index+6+i],16);
		}
		
		/* RSSI2 */
		if(scan.contains("RSSI2")) {
			for(index = 0; index < data.length; index++)
				if(data[index].equals("RSSI2")) break;

			angleStepWidth = Integer.parseInt(data[index+4],16);
			rssi2 = new int[numData = Integer.parseInt(data[index+5],16)];
			for(int i = 0; i < numData; i++)
				rssi2[i]=Integer.parseInt(data[index+6+i],16);
		}
		
		return true;
	}
	
	/**
	 * 
	 * @param knownPose The given position information from the file
	 * @param dists A string of all the distances separated by spaces
	 * @param remissions A string of all the remission values separated by spaces
	 * @return
	 */
	public boolean loadScan(String dists, String remissions, int angularFreq){
		int numData = (int)(270 / angularFreq);
		String nextTok;
		StringTokenizer parsedDists, parsedRssi;
		
		if (dists == null || remissions == null)
			return false;
		/* Break up distance string by spaces */
		parsedDists = new StringTokenizer(dists, " ");
		
		/* Ignore the first value (which is 0000000) */
		if (parsedDists.hasMoreTokens())
			parsedDists.nextToken();
		else
			return false;
		
		if (numData != parsedDists.countTokens() - 1)
			return false;
		dist1 = new int[numData];
		nextTok = null;
		
		/* Fill the dist array with the tokenized values */
		for (int i = 0; i < numData; i++){
			nextTok = parsedDists.nextToken();
			
			/*  Convert from hex to long */
			long result = 0;
			for(int j = 0; j < nextTok.length(); j++) {
				result = (result << 4) | Character.digit(nextTok.charAt(j), 16);
			}
			this.dist1[i] = (int)result;
		}
		
		/* Break up the rssi values by spaces */
		parsedRssi = new StringTokenizer(remissions, " ");
		
		if (parsedRssi.hasMoreTokens())
			parsedRssi.nextToken();
		else
			return false;
		
		if (numData != parsedRssi.countTokens() - 1)
			return false;
		rssi1 = new int[numData];
		nextTok = null;
		
		/* Fill the rssi array with the tokenized values */
		for (int i = 0; i < numData; i++){
			nextTok = parsedRssi.nextToken();
			
			/*  Convert from hex to long */
			long result = 0;
			for(int j = 0; j < nextTok.length(); j++) {
				result = (result << 4) | Character.digit(nextTok.charAt(j), 16);
			}
			this.rssi1[i] = (int)result;
		}
		
		return true;
	}
	
	/**
	 * Returns a string with all the information regarding this instance of
	 * ScanData.
	 * @return string for ScanData
	 */
	public String toString(){
		StringBuffer sb = new StringBuffer(); /* using string buffer for more efficient appending */		
		
		sb.append(	"Device Status: " 	+ deviceStatus 	+ "\n" +
					"Angle Step Width: "+ angleStepWidth+ "\n");
		
		sb.append("Range: ");
		for(int i = 0 ; i < dist1.length ; i++) sb.append(dist1[i]+" ");
		sb.append("\n");
		
		sb.append("Remission: ");
		for(int i = 0 ; i < rssi1.length ; i++)	sb.append(rssi1[i]+" ");
		sb.append("\n");
		
		return sb.toString();
	}
}