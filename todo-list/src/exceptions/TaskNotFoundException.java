package exceptions;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(String taskId) {
        super("No task found with id: " + taskId);
    }
}
