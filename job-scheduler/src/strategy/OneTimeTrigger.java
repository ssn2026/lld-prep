package strategy;

public class OneTimeTrigger implements Trigger {
    private final long delayMillis;

    public OneTimeTrigger(long delayMillis) {
        this.delayMillis = delayMillis;
    }

    @Override
    public long firstExecutionTime(long scheduledAtMillis) {
        return scheduledAtMillis + delayMillis;
    }

    @Override
    public long nextExecutionTime(long previousExecutionMillis) {
        throw new UnsupportedOperationException("OneTimeTrigger never recurs");
    }

    @Override
    public boolean isRecurring() {
        return false;
    }
}
