package state;

import exceptions.InvalidTaskTransitionException;
import model.Task;
import model.TaskStatus;

/**
 * One Task's own lifecycle, held per-instance on the Task itself (unlike
 * the ATM's AtmState, which is held once on the single AtmService since
 * there's only one physical machine -- here every Task has an independent
 * lifecycle, so every Task holds its own current state reference).
 * Concrete states are stateless singletons; only default (throwing)
 * transitions are overridden per state, matching AtmState's shape.
 */
public interface TaskState {
    TaskStatus getStatus();

    default TaskState start(Task task) {
        throw new InvalidTaskTransitionException(getStatus(), "start");
    }

    default TaskState complete(Task task) {
        throw new InvalidTaskTransitionException(getStatus(), "complete");
    }

    default TaskState reopen(Task task) {
        throw new InvalidTaskTransitionException(getStatus(), "reopen");
    }

    default TaskState archive(Task task) {
        throw new InvalidTaskTransitionException(getStatus(), "archive");
    }
}
