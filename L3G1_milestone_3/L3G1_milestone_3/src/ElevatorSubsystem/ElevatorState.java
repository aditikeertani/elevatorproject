package ElevatorSubsystem;

/**
 * Represents a state in the elevator system.
 * Defines a method to handle elevator events.
 *
 * @author Nkechi Chukwuma, Aditi Keertani
 */
public interface ElevatorState {
    /**
     * Handles an elevator event.
     *
     * @param event    The event to handle.
     * @param elevator The elevator associated with the event.
     */
    void handleEvent(ElevatorEvent event, Elevator elevator);
}
