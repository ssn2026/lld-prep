package state;

import model.Task;
import model.TaskStatus;

public class TodoState implements TaskState {
    public static final TodoState INSTANCE = new TodoState();

    private TodoState() {
    }

    @Override
    public TaskStatus getStatus() {
        return TaskStatus.TODO;
    }

    @Override
    public TaskState start(Task task) {
        return InProgressState.INSTANCE;
    }

    @Override
    public TaskState complete(Task task) {
        return DoneState.INSTANCE;
    }
}
