package exceptions;

public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException(String jobId) {
        super("No job found with id: " + jobId);
    }
}
