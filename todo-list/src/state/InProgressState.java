package state;

import model.Task;
import model.TaskStatus;

public class InProgressState implements TaskState {
    public static final InProgressState INSTANCE = new InProgressState();

    private InProgressState() {
    }

    @Override
    public TaskStatus getStatus() {
        return TaskStatus.IN_PROGRESS;
    }

    @Override
    public TaskState complete(Task task) {
        return DoneState.INSTANCE;
    }
}
