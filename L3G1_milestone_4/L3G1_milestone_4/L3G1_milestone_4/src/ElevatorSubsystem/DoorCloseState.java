package ElevatorSubsystem;

/**
 * Represents the state of the elevator when the door is closed.
 * @author Nkechi Chukwuma, Aditi Keertani
 */
public class DoorCloseState implements ElevatorState {

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
            // Transition to idle state after door opens
            elevator.setState(new IdleState());
        } else if (event == ElevatorEvent.STOP) {
            // Stop the elevator if commanded to stop
            elevator.stop();
        }
    }
}

