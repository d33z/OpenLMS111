
package lidar;

import java.awt.Point;

import java.awt.Polygon;
import java.text.DecimalFormat;

/**
 * Position class, main method of tracking the robot's position
 * 
 * @author alg2
 *
 */
public class Position implements Cloneable{
	public double x;
	public double y;
	public double heading;/* in angles */

	public Position(double x, double y){
		this.x = x;
		this.y = y;
		this.heading = 0;
	}
	public Position(double x, double y, double heading) {
		this.x = x;
		this.y = y;
		this.heading = (heading%360 + 360)%360;
	}
	
	@Override
	public String toString() {
		DecimalFormat df = new DecimalFormat("000");
		return df.format(x) + " " + df.format(y) + " " + df.format(heading);	
	}

	/**
	 * Computes distance from position stored vs the incoming position.
	 * NOTE: This will always be positive.
	 * 
	 * @param toPosition
	 * @return positive distance between the positions
	 * 
	 */
	public double getDistance(Position newPosition){
		return Math.sqrt(	Math.pow(newPosition.y - this.y, 2.0) + 
							Math.pow(newPosition.x - this.x, 2.0) );
	}
	
	/**
	 * Computes the number of degrees needed to turn from the current position
	 * to face goal position.
	 * If goal position is the same as this position, angle = 0
	 * 
	 * @param goal Our goal position.
	 * @return Returns Number of degrees needed expressed in range [-180,180]
	 */
	public double computeThetaDeltaToGoal(Position goal) {
		double hyp = getDistance(goal);
		double diffx = goal.x - x;
		double diffy = goal.y - y;
		double angle, thetaToTurn;
		
		/*If we're there, we'll define the angle to be 0*/
		if(diffx == 0 && diffy == 0) 
			return 0;

		/*Compute angle from rover to goal*/
		 if(diffx >= 0) {
			if(diffy >= 0) {
				angle = 180-(Math.asin(diffx/hyp))*180/(Math.PI);
			} else {
				angle = 0+(Math.asin(diffx/hyp))*180/(Math.PI);
			}
		} else {
			if(diffy >= 0) {
				angle = 180+(Math.asin(-diffx/hyp))*180/(Math.PI);
			} else {
				angle = 360-(Math.asin(-diffx/hyp))*180/(Math.PI);
			}
		}
		
		/*Compute difference between heading and angle in range [-180,180]*/
		thetaToTurn = angle - heading;
		if(thetaToTurn > 180) return thetaToTurn - 360;
		if(thetaToTurn < -180) return thetaToTurn + 360;
		return thetaToTurn;
	}
	
	/**
	 * Computes the number of degrees needed to turn from the current position
	 * to face goal position in the least amount of degrees to orient EITHER
	 * the front ray OR the back ray.
	 * If goal position is the same as this position, angle = 0
	 * 
	 * @param goal Our goal position .
	 * @return Returns Number of degrees needed expressed in range [-180,180]
	 */
	public double computeThetaToRotate(Position goal) {
		double thetaToRotate = computeThetaDeltaToGoal(goal);
		
		if(thetaToRotate > 90) 
			thetaToRotate = -180+thetaToRotate;
		if(thetaToRotate < -90) 
			thetaToRotate = 180+thetaToRotate;
		return thetaToRotate;
	}
	
	

	/**
	 * computes the difference between the angles to know which way to turn
	 * right hand rule for number of degrees to turn (positive to counter-
	 * clockwise, negative for clockwise)
	 * 
	 * @param goal Our goal position
	 * @return number of degrees needed -180 to 180
	 */
	public double computeHeadingDelta(Position goal) {
		
		double delta = goal.heading - heading;
		
		if (delta>180){
			delta = delta -360;
		}
		
		if (delta < -180){
			delta = delta + 360;
		}
		
		if(delta == -180){
			return 180;
		}
		return delta;
	}
	
	/**
	 * Computes angle from position stored vs the incoming position.
	 * NOTE: This will always be positive.
	 * 
	 * @param toPosition
	 * @return positive distance between the positions
	 * 
	 */
	public double getSchmAngle(Position newPosition){
		double angle = absAngle(newPosition.heading) - absAngle(this.heading);
	
		if(angle > 180) return 360.0 - angle;
		else if(angle < -180) return 360.0 + angle;
		else return angle;
	}
	
	
	private double absAngle(double angle){
		if(angle > 360.0) return angle-360.0;
		else if(angle < 0 ) return angle+360.0;
		else return angle;
	}
	

	/**
	 * Updates position by distance
	 * @param dist Distance to travel
	 * @return current position
	 */
	public Position addDistance(double dist){
		x = x + Math.cos(Math.toRadians(heading)) * dist;
		y = y + Math.sin(Math.toRadians(heading)) * dist;
		return this;
	}

	/**
	 * Updates the current heading by the specified angle
	 * Note: Range of updated heading values is [0,360)
	 * @param angle
	 * @return updated position
	 */
	public void addAngle(double angle){
		this.heading = (heading + angle + 360) % 360;
	}

	
	public boolean equals(Position pose2){
		final double THRESHOLD = 0.001;
		
		if (Math.abs((this.y - pose2.y)/this.y) > THRESHOLD)
			return false;
		
		if (Math.abs((this.x - pose2.x)/this.x) > THRESHOLD)
			return false;
		
		if (Math.abs((this.heading - pose2.heading)/this.heading) > THRESHOLD)
			return false;
		
		return true;
	}
	
	//TODO: Update these 4 below to actually work. Should be the same code as
	//		is used in the UI to compute where the corners of the rover are to
	//		draw its picture on the map.
	/**
	 * Returns the position of the front left most point of the rover.
	 * @return
	 */
	public Position frontLeftCorner() {
		Position pose = clone();
		pose.x -= 2;
		pose.y += 2;
		return pose;	
	}

	/**
	 * Returns the position of the front right most point of the rover.
	 * @return
	 */
	public Position frontRightCorner() {
		Position pose = clone();
		pose.x += 2;
		pose.y -= 2;
		return pose;
	}

	/**
	 * Returns the position of the back left most point of the rover.
	 * @return
	 */
	public Position backLeftCorner() {
		Position pose = clone();
		pose.x -= 2;
		pose.y -= 2;
		return pose;	
	}
	
	/**
	 * Returns the position of the back right most point of the rover.
	 * @return
	 */
	public Position backRightCorner() {
		Position pose = clone();
		pose.x += 2;
		pose.y += 2;
		return pose;
	}
	
	public Polygon toRoverPolygon() {
		int[] xpoints = {(int) backLeftCorner().x, (int) frontLeftCorner().x, 
				(int) frontRightCorner().x, (int) backRightCorner().x};
		int[] ypoints = {(int) backLeftCorner().y, (int) frontLeftCorner().y, 
				(int) frontRightCorner().y, (int) backRightCorner().y,};

		return new Polygon(xpoints, ypoints, xpoints.length);
	}
	

	public Point toPoint() {
		return new Point((int)x, (int)y);
	}
	
	@Override
	public Position clone(){
		return new Position(this.x,this.y,this.heading);
	}
}
