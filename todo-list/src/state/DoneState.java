package state;

import model.Task;
import model.TaskStatus;

public class DoneState implements TaskState {
    public static final DoneState INSTANCE = new DoneState();

    private DoneState() {
    }

    @Override
    public TaskStatus getStatus() {
        return TaskStatus.DONE;
    }

    @Override
    public TaskState reopen(Task task) {
        return TodoState.INSTANCE;
    }

    @Override
    public TaskState archive(Task task) {
        return ArchivedState.INSTANCE;
    }
}
