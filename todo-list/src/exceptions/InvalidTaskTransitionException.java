package exceptions;

import model.TaskStatus;

public class InvalidTaskTransitionException extends RuntimeException {
    public InvalidTaskTransitionException(TaskStatus currentStatus, String action) {
        super("Cannot " + action + " a task that is " + currentStatus);
    }
}
