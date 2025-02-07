/**
 * The Floor class represents a floor in a building, containing information about its number,
 * the number of elevators it serves, and the state of its up and down lamps.
 *
 * The class also defines an enum type Direction, representing the possible states of the lamps
 * (UP, DOWN, or IDLE).
 *
 * @author Aditi Keertani
 */
package FloorSubSystem;

public class Floor {

	int floorNumber;
	int numElevators;
	boolean upLamp;
	boolean downLamp;
	boolean isUpperFloor;
	boolean isLowerFloor;

	/**
	 * Enum type representing the possible directions of the lamps: UP, DOWN, or IDLE.
	 */
	enum Direction {
		UP,
		DOWN,
		IDLE
	}

	Direction[] lampDirection;

	/**
	 * Constructs a Floor object with the specified parameters.
	 *
	 * @param floorNumberber      The floor number.
	 * @param numElev      The number of elevators serving the floor.
	 * @param upper        Indicates if the floor is an upper floor.
	 * @param lower        Indicates if the floor is a lower floor.
	 */
	public Floor(int floorNumberber, int numElev, boolean upper, boolean lower) {
		floorNumber = floorNumberber;
		numElevators = numElev;
		upLamp = false;
		downLamp = false;
		lampDirection = new Direction[numElev];
		for (int i = 0; i < numElev; i++) lampDirection[i] = Direction.IDLE;
	}

	/**
	 * Returns the floor number.
	 *
	 * @return The floor number.
	 */
	public int getFloorNumber(){
		return (this.floorNumber);
	}

	/**
	 * Returns the number of elevators serving the floor.
	 *
	 * @return The number of elevators.
	 */
	public int getElevNum() {
		return (this.numElevators);
	}

	/**
	 * Returns the direction of the lamps (UP, DOWN, or IDLE).
	 *
	 * @return The direction of the lamps.
	 */
	public Direction getDirection() {
		return (this.lampDirection[0]);
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
