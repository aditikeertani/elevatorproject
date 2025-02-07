package ElevatorSubsystem;

/**
 * Represents an elevator.
 * Implements Runnable to allow it to run concurrently.
 * Manages elevator states and actions.
 *
 * @author Ryan Capstick
 * @author Oluwatobi Olowookere
 */
public class Elevator implements Runnable {
	public String currentFloor;
	public ElevatorButton direction;
	int[] Lamp = new int[MAX_FLOOR];
	private static final int MAX_FLOOR = 22;

	public ElevatorStateMachine stateMachine;
	public ElevatorState cState;

	/**
	 * Constructs an Elevator object with the given initial floor and direction.
	 *
	 * @param currentFloor The current floor of the elevator.
	 * @param direction    The direction the elevator is moving.
	 */
	public Elevator(String currentFloor, ElevatorButton direction) {
		this.currentFloor = currentFloor;
		this.direction = direction;
		stateMachine = new ElevatorStateMachine();

		for (int i = 0; i < MAX_FLOOR; i++) {
			Lamp[i] = 0;
		}
		cState = stateMachine.getCurrentState();
	}

	/**
	 * Runs the elevator's logic to move between floors.
	 */
	@Override
	public void run() {
		int pos = 0;
		try {
			pos = Integer.parseInt(this.currentFloor);
		} catch (NumberFormatException e) {
			System.out.println("ELEVATOR: ERROR current floor");
		}

		if (direction == ElevatorButton.UP && pos < MAX_FLOOR) {
			// wait 6 seconds and move
			try {
				Thread.sleep(6000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			}
			pos += 1;
			this.currentFloor = Integer.toString(pos);
			// Send pos
		} else if (direction == ElevatorButton.DOWN && pos > 1) {
			// wait 6 seconds and move
			try {
				Thread.sleep(6000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			}
			pos -= 1;
			this.currentFloor = Integer.toString(pos);
			// send pos
		} else if (direction == ElevatorButton.HOLD) {
			this.currentFloor = this.getCurrentFloor();
		} else {
			System.out.println("ELEVATOR: ERROR elevator direction");
		}
	}

	/**
	 * Gets the current floor of the elevator.
	 *
	 * @return The current floor.
	 */
	public String getCurrentFloor() {
		return this.currentFloor;
	}

	/**
	 * Gets the current floor as an integer.
	 *
	 * @return The current floor as an integer.
	 */
	public int getIntFloor() {
		int pos = 0;
		try {
			pos = Integer.parseInt(this.currentFloor);
		} catch (NumberFormatException e) {
			System.out.println("ELEVATOR: ERROR current floor");
		}
		return pos;
	}

	/**
	 * Gets the direction of the elevator.
	 *
	 * @return The direction of the elevator.
	 */
	public ElevatorButton getDirection() {
		return this.direction;
	}

	/**
	 * Gets the status of the elevator.
	 *
	 * @return The status of the elevator.
	 */
	public ElevatorStatus getStatus() {
		return (this.getDirection().equals(ElevatorButton.HOLD)) ? ElevatorStatus.EMPTY : ElevatorStatus.IN_USE;
	}

	/**
	 * Sets the direction of the elevator.
	 *
	 * @param direction The direction to set.
	 */
	public void setDirection(ElevatorButton direction) {
		this.direction = direction;
	}

	/**
	 * Sets the state of the elevator.
	 *
	 * @param state The state to set.
	 */
	public void setState(ElevatorState state) {
		stateMachine.setState(state);
	}

	/**
	 * Moves the elevator based on its current direction.
	 */
	public void move() {
		if (direction == ElevatorButton.UP) {
			cState.handleEvent(ElevatorEvent.GOING_UP, this);
		} else if (direction == ElevatorButton.DOWN) {
			cState.handleEvent(ElevatorEvent.GOING_DOWN, this);
		} else {
			System.out.println("ELEVATOR: ERROR invalid direction for movement");
		}
	}

	/**
	 * Stops the elevator.
	 */
	public void stop() {
		this.currentFloor = this.getCurrentFloor();
	}

	/**
	 * Opens the door of the elevator.
	 *
	 * @param num_elevator The number of the elevator.
	 */
	public void open(int num_elevator) {
		cState.handleEvent(ElevatorEvent.OPEN_DOOR, this);
		System.out.println("ELEVATOR " + num_elevator + ": Door OPENED at " + this.getCurrentFloor());
	}

	/**
	 * Closes the door of the elevator.
	 *
	 * @param num_elevator The number of the elevator.
	 */
	public void close(int num_elevator) {
		cState.handleEvent(ElevatorEvent.CLOSE_DOOR, this);
		System.out.println("ELEVATOR " + num_elevator + ": Door CLOSED at " + this.getCurrentFloor());
	}
}
