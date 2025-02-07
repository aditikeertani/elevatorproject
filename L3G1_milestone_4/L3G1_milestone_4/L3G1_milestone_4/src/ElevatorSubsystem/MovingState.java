package ElevatorSubsystem;

/**
 * Represents the state of the elevator when it is moving.
 * @author Nkechi Chukwuma, Aditi Keertani
 */
public class MovingState implements ElevatorState {

    /**
     * Handles events related to the elevator's state.
     *
     * @param event    The event triggered.
     * @param elevator The elevator instance.
     */
    @Override
    public void handleEvent(ElevatorEvent event, Elevator elevator) {
        if (event == ElevatorEvent.OPEN_DOOR) {
            try {
                // Simulate door opening delay
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                // Terminate program if sleep is interrupted
                System.exit(1);
            }
            // Transition to door open state after door opens
            elevator.setState(new DoorOpenState());
        } else if (event == ElevatorEvent.CLOSE_DOOR) {
            try {
                // Simulate door closing delay
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                // Terminate program if sleep is interrupted
                System.exit(1);
            }
            // Transition to door close state after door closes
            elevator.setState(new DoorCloseState());
        } else if (event == ElevatorEvent.STOP) {
            // Stop the elevator if commanded to stop
            elevator.stop();
        }
    }
}

