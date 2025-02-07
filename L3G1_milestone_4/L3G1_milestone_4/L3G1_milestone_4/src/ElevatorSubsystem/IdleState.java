package ElevatorSubsystem;

/**
 * Represents the idle state of the elevator.
 * Handles events such as going up, going down, opening door, and stopping.
 * Transitions to moving state when required.
 *
 * @author Nkechi Chukwuma, Aditi Keertani
 */
public class IdleState implements ElevatorState {
    @Override
    public void handleEvent(ElevatorEvent event, Elevator elevator) {
        if (event == ElevatorEvent.GOING_UP) {
            elevator.setDirection(ElevatorButton.UP);
            elevator.run();
            elevator.setState(new MovingState());
        } else if (event == ElevatorEvent.GOING_DOWN) {
            elevator.setDirection(ElevatorButton.DOWN);
            elevator.move();
            elevator.setState(new MovingState());
        } else if (event == ElevatorEvent.OPEN_DOOR) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else if (event == ElevatorEvent.STOP) {
            elevator.stop();
        }
    }
}
