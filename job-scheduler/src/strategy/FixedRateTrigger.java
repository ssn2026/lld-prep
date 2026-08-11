package strategy;

public class FixedRateTrigger implements Trigger {
    private final long initialDelayMillis;
    private final long intervalMillis;

    public FixedRateTrigger(long initialDelayMillis, long intervalMillis) {
        this.initialDelayMillis = initialDelayMillis;
        this.intervalMillis = intervalMillis;
    }

    @Override
    public long firstExecutionTime(long scheduledAtMillis) {
        return scheduledAtMillis + initialDelayMillis;
    }

    @Override
    public long nextExecutionTime(long previousExecutionMillis) {
        return previousExecutionMillis + intervalMillis;
    }

    @Override
    public boolean isRecurring() {
        return true;
    }
}
