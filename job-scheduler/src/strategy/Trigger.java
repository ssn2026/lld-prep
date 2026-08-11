package strategy;

/**
 * Decides *when* a job should next run. Kept separate from Job (the "what")
 * so new scheduling policies (cron-style, business-hours-only, etc.) can be
 * added without touching Job or JobSchedulerService.
 */
public interface Trigger {
    long firstExecutionTime(long scheduledAtMillis);

    long nextExecutionTime(long previousExecutionMillis);

    boolean isRecurring();
}
