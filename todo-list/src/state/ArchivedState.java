package state;

import model.TaskStatus;

/**
 * Terminal: every transition falls through to the interface's throwing
 * defaults, same shape as ATM's CardRetainedState.
 */
public class ArchivedState implements TaskState {
    public static final ArchivedState INSTANCE = new ArchivedState();

    private ArchivedState() {
    }

    @Override
    public TaskStatus getStatus() {
        return TaskStatus.ARCHIVED;
    }
}
