package tools;

import java.awt.Color;

import java.awt.Dimension;
import java.awt.Graphics;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

import lidar.LMS111;
import lidar.Position;
import lidar.ScanData;

public class LMSVisualizationPanel extends JPanel{
	private int dotSize = 4;
	private double angleWeight=0;
	
	private int xcoordOffset = 250;
	private int ycoordOffset = 250;
	private int indicatorLength = 150;
	private int angleOffset = 135;
	private int distWeight = 1;
	
	private int[] dist;
	private int[] rssi;
	private LMS111 lms;
	private ScanData sd;
	
	public LMSVisualizationPanel(LMS111 lms){
		setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.black),
				"LMS Visualization"));
		this.lms = lms;
    }
	
	public LMSVisualizationPanel(ScanData sd){
		dist = sd.getDIST1();
		rssi = sd.getRSSI1();
		angleWeight = 270.0/dist.length;
		
		this.setEnabled(false);
	}
	
	@Override
	public void paint(Graphics g){
		super.paint(g);
		
		paintIndicators(g);
	
		if(this.isEnabled()) readLMS();
		
		paintPoints(g);
	}
	
	/**
	 * Reads a scan from the LMS and updates current dist and rssi values.
	 * Also, it sets the angleWeight which is angle equivalent of each
	 * element in the DIST/RSSI value in degrees.
	 */
	private void readLMS(){
		sd = lms.getScan();
		
		dist = sd.getDIST1();
		rssi = sd.getRSSI1();
		
		angleWeight = 270.0/dist.length;
	}
	
	private void paintIndicators(Graphics g){
		g.setColor(Color.black);
		
		g.drawLine(	xcoordOffset, ycoordOffset,
					xcoordOffset + (int) (indicatorLength * Math.cos(Math.toRadians(angleOffset))) ,
					ycoordOffset - (int) (indicatorLength * Math.sin(Math.toRadians(angleOffset))) );
		g.drawLine(	xcoordOffset, ycoordOffset,
					xcoordOffset + (int) (indicatorLength * Math.cos(Math.toRadians(angleOffset+270.0))) ,
					ycoordOffset - (int) (indicatorLength * Math.sin(Math.toRadians(angleOffset+270.0))) );
	}
	
	private void paintPoints(Graphics g){	
	   	for(int i = 0; i < dist.length ; i++)  {
     		g.fillOval(
    				xcoordOffset + (int) (dist[i]*distWeight * Math.cos(Math.toRadians(angleOffset + (i)*angleWeight))) -dotSize/2,
    				ycoordOffset - (int) (dist[i]*distWeight * Math.sin(Math.toRadians(angleOffset + (i)*angleWeight))) - dotSize/2,
    				dotSize,dotSize);
		}
	}
	
	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	ScanData sd = new ScanData(new int[] {100,200,200,200,200,200,200,200,200,200,200,200,200} , new int[] {100,200,200,200,200,200,200,200,200,200,200,200,200});

               	JFrame frame = new JFrame("LMS Visualizer");
                LMSVisualizationPanel v = new LMSVisualizationPanel(sd);
               	frame.getContentPane().add(v);
               	frame.setSize(500,500);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                v.repaint();
                frame.pack();                
                frame.setVisible(true);
                
            }
        });
    }
}