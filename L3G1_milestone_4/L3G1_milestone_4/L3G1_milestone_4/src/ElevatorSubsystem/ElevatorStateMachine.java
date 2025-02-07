package ElevatorSubsystem;

/**
 * Represents a state machine for the elevator system.
 * Manages the current state of the elevator.
 *
 * @author Nkechi Chukwuma, Aditi Keertani
 */
public class ElevatorStateMachine {
    private ElevatorState currentState;

    /**
     * Constructs an ElevatorStateMachine object with the initial state set to IdleState.
     */
    public ElevatorStateMachine() {
        currentState = new IdleState();
    }

    /**
     * Sets the current state of the state machine.
     *
     * @param state The new state to set.
     */
    public void setState(ElevatorState state) {
        currentState = state;
    }

    /**
     * Retrieves the current state of the state machine.
     *
     * @return The current state.
     */
    public ElevatorState getCurrentState() {
        return currentState;
    }
}
