package observer;

public class ConsoleJobListener implements JobListener {
    @Override
    public void onJobCompleted(String jobId) {
        System.out.println("[listener] job completed: " + jobId);
    }

    @Override
    public void onJobFailed(String jobId, Throwable error) {
        System.out.println("[listener] job failed: " + jobId + " -> " + error.getMessage());
    }
}
