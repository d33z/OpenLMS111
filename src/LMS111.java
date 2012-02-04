package lidar;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;


/**
 * Driver for SICK LMS-111.
 * Currently only supports communication over Ethernet.
 *
 * @author Matthew F. Delaney, S. Steven Kang
 */
public class LMS111 {
	private static final String DEFAULT_HOSTNAME = "192.168.0.1";
	private static final int DEFAULT_PORT = 2111;

	/* Information needed to set an appropriate user mode */
	private static final int CLIENT_LEVEL = 3;
	private static final String CLIENT_PASS = "F4724744";

	@SuppressWarnings("unused")
	private static final int ROOT_LEVEL = 2;
	@SuppressWarnings("unused")
	private static final String ROOT_PASS = "B21ACE26";

	private int userLevel = CLIENT_LEVEL;
	private String userPassword = CLIENT_PASS;

	/* internal variables needed to make a connection over ethernet*/
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	private String hostname;
	private int port;
	
	/*State variables*/
	private boolean isConnected = false;
	private boolean isTempGood = true;

	/* these values are fixed */
	private static final int startAngle = -450000;
	private static final int stopAngle = 2250000;

	/**
	 * Creates instance of LMS with the address DEFAULT_HOSTNAME:DEFAULT_PORT
	 */
	public LMS111 () {
		init(DEFAULT_HOSTNAME,DEFAULT_PORT);
	}

	/**
	 * Creates an instance of LMS with a given hostname and port number used
	 * for connections.
	 *
	 * @param host The ip address of LMS.
	 * @param port The port on which LMS is listening.
	 */
	public LMS111 (final String host, final int port) {
		init(host,port);
	}

	private void init(final String host, final int port){
		this.port = port;
		this.hostname = host;	
	}
	
	/**
	 * Opens a connection to LMS.  This method also sets the usermode
	 * to "Authorized Client" level by default.
	 * @return 	true if successfully connected
	 * 			false if connection could not be made or usermode change failed.
	 */
	public boolean connect () {
		if(isConnected) {
			System.err.println("This LMS111 is already connected.");
			return false;
		}

		Socket sock;
		PrintWriter out;
		BufferedReader in;

		try {
			sock = new Socket(this.hostname, this.port);
			out = new PrintWriter(sock.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		} catch (final UnknownHostException e) {
			System.err.println("Connect Failed: Unknown Host");
			e.printStackTrace();
			return false;
		} catch (final IOException e) {
			System.err.println("Connect Failed: I/O Error");
			e.printStackTrace();
			return false;
		}

		this.socket = sock;
		this.out = out;
		this.in = in;
		isConnected = true;
		
		/* set user mode.  The default is "Authorized client" */
		send("sMN SetAccessMode "+ this.userLevel +" "+ this.userPassword);
		if(receive().equals("sAN SetAccessMode 1")){
			this.send("sWN LMDscandatacfg 03 00 1 1 0 00 00 0 0 0 0 +1");
			this.receive();
			return true;
		} else {
			System.err.println("Connected, but could not change user-level.");
			return false;
		}
	}

	/**
	 * Disconnect from LMS.
	 * @return 	true if successfully disconnected
	 * 			false if it was never connected or if LMS111 cannot be reached.
	 */
	public boolean disconnect () {
		if(!isConnected) {
			System.err.println("This LMS111 is already disconnected.");
			return false;
		}

		if(this.in != null) {
			try {
				this.in.close();
			} catch (final IOException e) {}
		}

		if(this.out != null)
			this.out.close();

		if(this.socket != null) {
			try {
				this.socket.close();
			} catch (final IOException e) {}
		}

		this.in = null;
		this.out = null;
		this.socket = null;
		isConnected = false;
		return true;
	}

	/**
	 * Attempts to start LMS measuring.
	 *
	 * @return true on successful start of measuring, false otherwise
	 */
	public boolean startMeasuring() {
		final String command = "sMN LMCstartmeas";
		send(command);

		final String[] response = parseResponse(receive());
		final int error = Integer.parseInt(response[2]);

		return (error == 0);
	}

	/**
	 * Stop LMS measuring.
	 * @return true on successful stop of measuring, false otherwise
	 */
	public boolean stopMeasuring() {
		final String command = "sMN LMCstopmeas";
		send(command);

		final String[] response = parseResponse(receive());
		final int error = Integer.parseInt(response[2],16);

		return !(error == 1);
	}
	
	/**
	 * Gets the latest scan from the connected LMS11 in the form of a ScanData.
	 * @return Null on error, latest available scan information on success.
	 */
	public ScanData getScan() {
		String str = getRawScan();
		if(str == null) return null;
		
		try {
			return new ScanData(str);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Get the raw string version of the scan directly from the LMS111.
	 * @return
	 */
	public String getRawScan(){
		send("sRN LMDscandata");
		return receive();
	}

	/**
	 * Queries the status of LMS
	 * @return the status according to the documentation.  See page 86.
	 */
	public String[] queryStatus () {
		final String command = "sRN STlms";
		send(command);
		return parseResponse(receive());
	}

	/**
	 * Reports the operating status code.
	 *
	 * @return 	operating status code
	 */
	public int getStatusCode() {
		return Integer.parseInt(queryStatus()[2],16);
	}

	/**
	 * Reports if temperature of unit is in operating range.
	 * @return 	true if operating temp range is met
	 * 			false otherwise
	 */
	public boolean isTempGood () {
		isTempGood = (Integer.parseInt(queryStatus()[3],16) == 0);
		return isTempGood;
	}

	/**
	 * Returns the scanning frequency of the LMS in 1/100Hz.  It is always the
	 * case that the value of the scanning frequency is less than or equal to
	 * that of the angular resolution.
	 *
	 * @return 2500(25hz) or 5000(50hz).
	 */
	public int getScanFreq() {
		final String command = "sRN LMPscancfg";
		send(command);

		final String[] response = parseResponse(receive());
		final int freq = Integer.parseInt(response[2],16);

		if(!(freq == 2500 || freq == 5000)) {
			System.err.println("Unusual Scanning Frequency");
			return -1;
		}

		return freq;
	}

	/**
	 * Returns the angular resolution of the LMS in 1/10,000 degrees.  It is
	 * always the case that the value of the scanning frequency is greater than
	 * or equal to that of the scanning frequency.
	 *
	 * @return 2500(.25 degrees) or 5000(.50 degrees)
	 */
	public int getAngularRes() {
		final String command = "sRN LMPscancfg";
		send(command);

		final String[] response = parseResponse(receive());
		final int angularRes = Integer.parseInt(response[4],16);

		if(!(angularRes == 2500 || angularRes == 5000)) {
			System.err.println("Unusual Angular Resolution");
			return -1;
		}

		return angularRes;
	}

	/**
	 * Returns the starting angle -450000
	 * @return -4500000
	 */
	public int getStartAngle() {
		return startAngle;
	}

	/**
	 * Returns the stopping angle +2250000
	 * @return 22500000
	 */
	public int getStopAngle() {
		return stopAngle;
	}

	public int getContaminationLevel() {
		final String command = "sRN LCMstate";
		send(command);

		final String[] response = parseResponse(receive());
		final int contamLevel = Integer.parseInt(response[2],16);

		return contamLevel;
	}
	
	/**
	 * Reports whether or not the unit is ready to scan
	 *
	 * @return 	true if ready to scan
	 * 			false otherwise
	 */
	public boolean isScannable() {
		return (getStatusCode() == 7);
	}

	/**
	 * Configure's laser's scan frequency and the angle resolution
	 * @param scanFreq
	 * @param angleRes
	 * @return
	 */
	public boolean configureLaser(final int scanFreq, final int angleRes){
		final String cmd = 	"sMN mLMPsetscancfg " +
						formatInteger(scanFreq) +
						" +1 " + formatInteger(angleRes) +
						" " + formatInteger(startAngle) +
						" " + formatInteger(stopAngle);

		send(cmd);
		final String[] response = parseResponse(receive());

		/* error returned by LMS */
		final int error = Integer.parseInt(response[2],16);
		String errorMessage = "";

		if(error != 0){
			switch(error){
				case 1: errorMessage = "invalid frequency"; break;
				case 2: errorMessage = "invalid angular resolution"; break;
				case 3: errorMessage = "invalid frequency and angular resolution"; break;
				case 4: errorMessage = "invalid scan area"; break;
				default: errorMessage = "other error"; break;
			}
			System.err.println(errorMessage);
			return false;
		}

		return true;
	}

	/**
	 * Default values for configureScanOutput method.
	 * @return
	 */
	public boolean configureDefaultScanOutput() {
		String response;
		final String command = "sWN LMDscandatacfg 03 00 1 1 0 00 00 0 0 0 0 +1";
		send(command);
		response = receive();
		if(response.equals("sWA LMDscandatacfg"))
			return true;
		return false;
	}

	/**
	 * Configures the laser's scanoutput.
	 * For our purpose:
	 * 	outputChannel = 3;
	 *  remission = true;
	 *  resolution = true;
	 *  encoder = 0;
	 *  ... = false;
	 *  outputInterval = 1;
	 */
	public boolean configureScanOutput(	final int outputChannel,
										final boolean remission,
										final boolean resolution,
										final int encoder,
										final boolean position,
										final boolean deviceName,
										final boolean comment,
										final boolean time,
										final int outputInterval) {
		String response;
		/* packing command */
		String cmd = 	"sWN LMDscandatacfg " +

		/* not sure what "10 FF reserved" in v2 documentaiton is for */
		formatInteger(outputChannel) + " 00 ";

		if (remission) 	cmd += "+1 "; else cmd += "0 ";
		if (resolution) cmd += "1 "; else cmd += "0 ";
		/* encoder */	cmd += formatInteger(encoder) + " 00 ";
		if (position) 	cmd += "1 "; else cmd += "0 ";
		if (deviceName) cmd += "1 "; else cmd += "0 ";
		if (comment) 	cmd += "1 "; else cmd += "0 ";
		if (time) 		cmd += "1 "; else cmd += "0 ";
  	/*outputInterval*/	cmd += formatInteger(outputInterval);

		send(cmd);

		response = receive();

		if(!response.equals("sWA LMDscandatacfg")) return false;

		return true;
	}
	/**
	 * Sends a message to LMS according to the specification.  This method
	 * should only be used internally.
	 *
	 * @param cmd message to be sent.
	 */
	private boolean send (final String cmd) {
		if(!isConnected) {
			System.err.println("LMS111 is not connected!");
			return false;
		}
		
		this.out.write(0x02);
		for(int i = 0; i < cmd.length(); i++)
			this.out.write(cmd.charAt(i));
		this.out.write(0x03);
		this.out.write(0x00);
		this.out.flush();
		return true;
	}

	/**
	 * Retrieves a message from LMS.  This method should only be used internally
	 *
	 * @return message from the LMS.
	 */
	private String receive() {
		if(!isConnected) {
			System.err.println("LMS111 is not connected!");
			return null;
		}
		
		final StringBuffer buf = new StringBuffer();
		int c = 0;

		/*Find STX*/
		do
			try {
				c = this.in.read();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		while (c == 0);

		/*Read in message*/
		while(true){
			/*Read in character*/
			try {
				c = this.in.read();
			} catch (final IOException e) {
				e.printStackTrace();
			}

			/*End of stream*/
			if(c == -1)
				break;

			/*Append character if not an ETX*/
			if(c == 0x03)
				break;
			else
				buf.append((char)c);
		}
		return buf.toString();
	}

	/**
	 * Divides the messages into an array of strings.
	 *
	 * @param response a message received from LMS
	 * @return an array of strings
	 */
	private static String[] parseResponse (final String response) {
		return response.trim().split(" ");
	}

	/**
	 * Format signed integers.  Add + for positive non-hex integers in
	 * accordance with the specification.
	 *
	 * @param num the integer to be formatted
	 * @return string form of the number with "+" if positive.
	 */
	private static String formatInteger(final int num){
		if(num >= 0) return "+"+Integer.toString(num);
		else return Integer.toString(num);
	}


	/**
	 * Returns the date and time reported by the unit.
	 * TODO: look into supposed deprecated date functions
	 *
	 * @return
	 */
	@Deprecated
	public Date getUnitDate () {
		String[] date=null, time=null;
		final String[] status = queryStatus();
		time = status[5].split(":");
		date = status[7].split("\\.");

		//TODO(mdelaney): Make sure that we're getting the order of year, month,
		//					day, etc. correct.  Also, check if they are hex.
		final int year = Integer.parseInt(date[0]);
		final int month = Integer.parseInt(date[1]);
		final int day = Integer.parseInt(date[2]);
		final int hour = Integer.parseInt(time[0]);
		final int minute = Integer.parseInt(time[1]);
		final int second = Integer.parseInt(time[2]);

		return new Date(year, month, day, hour, minute, second);
	}
}
