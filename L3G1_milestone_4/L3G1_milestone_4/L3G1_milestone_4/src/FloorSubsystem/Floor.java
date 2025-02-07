package FloorSubsystem;

/**
 * Represents a floor in a building.
 *
 * @author Nkechi Chukwuma, Aditi Keertani
 */
public class Floor {

	int floorNum;
	int numElevators;
	boolean upLamp;
	boolean downLamp;
	boolean isUpperFloor;
	boolean isLowerFloor;

	enum Direction {
		UP,
		DOWN,
		IDLE
	}

	Direction[] lampDirection;

	/**
	 * Constructs a Floor object with specified parameters.
	 *
	 * @param numElev Number of elevators on the floor.
	 * @param floorNo Floor number.
	 * @param upper   Whether it's an upper floor.
	 * @param lower   Whether it's a lower floor.
	 */
	public Floor(int numElev, int floorNo, boolean upper, boolean lower) {
		floorNum = floorNo;
		numElevators = numElev;
		upLamp = false;
		downLamp = false;
		lampDirection = new Direction[numElev];
		for (int i = 0; i < numElev; i++)
			lampDirection[i] = Direction.IDLE;
	}

	public int getFloorNum() { return(this.floorNum); }
	public int getElevNum() { return(this.numElevators); }
	public boolean getUpLampStatus() { return(this.upLamp); }
	public boolean getDownLampStatus() { return(this.downLamp); }

	/**
	 * Gets the direction of the lamps.
	 *
	 * @return The direction of the lamps.
	 */
	public Direction getDirection() { return(this.lampDirection[0]); }

	/**
	 * Turns on the up lamp.
	 */
	public void setUpLampOn() { upLamp = true; }

	/**
	 * Turns on the down lamp.
	 */
	public void setDownLampOn() { downLamp = true; }

	/**
	 * Turns off the up lamp.
	 */
	public void setUpLampOff() { upLamp = false; }

	/**
	 * Turns off the down lamp.
	 */
	public void setDownLampOff() { downLamp = false; }

}
