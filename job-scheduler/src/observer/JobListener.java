package observer;

/**
 * Observer pattern: gets notified of job outcomes without the scheduler
 * knowing or caring who's listening (logging, metrics, alerting, ...).
 */
public interface JobListener {
    void onJobCompleted(String jobId);

    void onJobFailed(String jobId, Throwable error);
}
