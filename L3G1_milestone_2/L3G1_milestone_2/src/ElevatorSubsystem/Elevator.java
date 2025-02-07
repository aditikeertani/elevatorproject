package ElevatorSubsystem;

/**
 * The Elevator class represents an elevator object in the Elevator System.
 * It implements the Runnable interface to allow for concurrent movement and action.
 *
 * @author Ryan Capstick
 * @author Oluwatobi Olowookere
 */
public class Elevator implements Runnable {
	public String currentFloor; // Current floor of the elevator
	public ElevatorButton direction; // Direction in which the elevator is moving

	// Maximum floor number
	private static final int MAX_FLOOR = 4;

	/**
	 * Constructs an Elevator object with the specified current floor and direction.
	 *
	 * @param currentFloor The current floor of the elevator.
	 * @param direction    The direction in which the elevator is moving (UP, DOWN, or HOLD).
	 */
	public Elevator(String currentFloor, ElevatorButton direction) {
		this.currentFloor = currentFloor;
		this.direction = direction;
	}

	/**
	 * The run method is used for the elevator's thread to make it move and act.
	 */
	@Override
	public void run() {
		int position = 0;
		try {
			position = Integer.parseInt(this.currentFloor);
		} catch (NumberFormatException e) {
			System.out.println("ELEVATOR: ERROR current floor");
		}

		if (direction == ElevatorButton.UP && position < MAX_FLOOR) {
			// wait 6 seconds and move
			try {
				Thread.sleep(6000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			}
			position += 1;
			this.currentFloor = Integer.toString(position);
			// Send position
		} else if (direction == ElevatorButton.DOWN && position > 1) {
			// wait 6 seconds and move
			try {
				Thread.sleep(6000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			}
			position -= 1;
			this.currentFloor = Integer.toString(position);
			// send position
		} else if (direction == ElevatorButton.HOLD) {
			this.currentFloor = this.getCurrentFloor();
		} else {
			System.out.println("ELEVATOR: ERROR elevator direction");
		}
	}

	/**
	 * Returns the location of the elevator.
	 *
	 * @return The current floor of the elevator.
	 */
	public String getCurrentFloor() {
		return this.currentFloor;
	}

	/**
	 * Returns the location of the elevator as an integer.
	 *
	 * @return The current floor of the elevator as an integer.
	 */
	public int getIntFloor() {
		int position = 0;
		try {
			position = Integer.parseInt(this.currentFloor);
		} catch (NumberFormatException e) {
			System.out.println("ELEVATOR: ERROR current floor");
		}
		return position;
	}

	/**
	 * Returns the current direction of the elevator.
	 *
	 * @return The current direction of the elevator (UP, DOWN, or HOLD).
	 */
	public ElevatorButton getDirection() {
		return this.direction;
	}

	/**
	 * Returns the current status of the elevator.
	 *
	 * @return The status of the elevator (IN_USE if moving, EMPTY if stopped).
	 */
	public ElevatorStatus getStatus() {
		return (this.getDirection().equals(ElevatorButton.HOLD)) ? ElevatorStatus.EMPTY : ElevatorStatus.IN_USE;
	}

	/**
	 * Opens the door of the elevator.
	 */
	public void open() {
		// wait 2 seconds and open door
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("ELEVATOR: Door OPENED at " + this.getCurrentFloor());
	}

	/**
	 * Closes the door of the elevator.
	 */
	public void close() {
		// wait 2 seconds and close door
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("ELEVATOR: Door CLOSED at " + this.getCurrentFloor());
	}
}
