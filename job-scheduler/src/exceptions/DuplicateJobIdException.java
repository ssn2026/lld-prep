package exceptions;

public class DuplicateJobIdException extends RuntimeException {
    public DuplicateJobIdException(String jobId) {
        super("A job is already registered with id: " + jobId);
    }
}
