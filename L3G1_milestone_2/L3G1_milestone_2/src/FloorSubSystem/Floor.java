package FloorSubSystem;

/**
 * The Floor class represents a floor in a building, containing information about its number,
 * the number of elevators it serves, and the state of its up and down lamps.
 * The class also defines an enum type Direction, representing the possible states of the lamps
 * (UP, DOWN, or IDLE).
 *
 * @author Aditi Keertani
 */
public class Floor {

	// Instance variables
	int floorNumber;
	int numElevators;
	boolean upLamp;
	boolean downLamp;
	boolean isUpperFloor;
	boolean isLowerFloor;

	// Enum type representing the possible directions of the lamps: UP, DOWN, or IDLE
	enum Direction {
		UP,
		DOWN,
		IDLE
	}

	// Array to hold lamp direction for each elevator
	Direction[] lampDirection;

	/**
	 * Constructs a Floor object with the specified parameters.
	 *
	 * @param floorNumber The floor number.
	 * @param numElevators The number of elevators serving the floor.
	 * @param isUpperFloor Indicates if the floor is an upper floor.
	 * @param isLowerFloor Indicates if the floor is a lower floor.
	 */
	public Floor(int floorNumber, int numElevators, boolean isUpperFloor, boolean isLowerFloor) {
		this.floorNumber = floorNumber;
		this.numElevators = numElevators;
		this.upLamp = false;
		this.downLamp = false;
		this.isUpperFloor = isUpperFloor;
		this.isLowerFloor = isLowerFloor;
		// Initializing lamp directions for each elevator to IDLE
		lampDirection = new Direction[numElevators];
		for (int i = 0; i < numElevators; i++) {
			lampDirection[i] = Direction.IDLE;
		}
	}

	/**
	 * Returns the floor number.
	 *
	 * @return The floor number.
	 */
	public int getFloorNumber() {
		return floorNumber;
	}

	/**
	 * Returns the number of elevators serving the floor.
	 *
	 * @return The number of elevators.
	 */
	public int getElevNum() {
		return numElevators;
	}

	/**
	 * Returns the direction of the lamps (UP, DOWN, or IDLE).
	 *
	 * @return The direction of the lamps.
	 */
	public Direction getDirection() {
		return lampDirection[0]; // Assuming only one elevator is considered for direction
	}

	/**
	 * Turns on the UP lamp for the floor.
	 */
	public void setUpLampOn() {
		upLamp = true;
	}

	/**
	 * Turns on the DOWN lamp for the floor.
	 */
	public void setDownLampOn() {
		downLamp = true;
	}

	/**
	 * Turns off the UP lamp for the floor.
	 */
	public void setUpLampOff() {
		upLamp = false;
	}

	/**
	 * Turns off the DOWN lamp for the floor.
	 */
	public void setDownLampOff() {
		downLamp = false;
	}
}
