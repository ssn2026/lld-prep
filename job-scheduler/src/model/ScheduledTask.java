package model;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import strategy.Trigger;

/**
 * One scheduled occurrence of a Job. Implements Delayed so it can live
 * inside a java.util.concurrent.DelayQueue, which hands elements back out
 * only once their delay has expired -- that's what lets the dispatcher
 * thread block efficiently instead of polling.
 *
 * executionTimeMillis is final: once an instance is sitting inside the
 * DelayQueue's internal heap, its ordering key must never change (same rule
 * as PriorityQueue). A recurring job's *next* occurrence is therefore a
 * brand-new ScheduledTask (see withNextExecution), not a mutation of this
 * one.
 */
public class ScheduledTask implements Delayed {
    private static final AtomicLong SEQUENCE_GENERATOR = new AtomicLong();

    private final String jobId;
    private final Job job;
    private final Trigger trigger;
    private final long executionTimeMillis;
    private final long sequence;
    private final AtomicReference<JobStatus> status = new AtomicReference<>(JobStatus.SCHEDULED);
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    public ScheduledTask(String jobId, Job job, Trigger trigger, long executionTimeMillis) {
        this.jobId = jobId;
        this.job = job;
        this.trigger = trigger;
        this.executionTimeMillis = executionTimeMillis;
        this.sequence = SEQUENCE_GENERATOR.incrementAndGet();
    }

    public ScheduledTask withNextExecution(long nextExecutionTimeMillis) {
        return new ScheduledTask(jobId, job, trigger, nextExecutionTimeMillis);
    }

    public String getJobId() {
        return jobId;
    }

    public Job getJob() {
        return job;
    }

    public Trigger getTrigger() {
        return trigger;
    }

    public long getExecutionTimeMillis() {
        return executionTimeMillis;
    }

    public JobStatus getStatus() {
        return status.get();
    }

    public boolean isCancelRequested() {
        return cancelRequested.get();
    }

    /**
     * Best-effort cancel. Always stops future recurrences; only stops THIS
     * occurrence if it hadn't started running yet (compareAndSet only
     * succeeds out of SCHEDULED). A run already in flight is left to finish.
     */
    public boolean cancel() {
        cancelRequested.set(true);
        return status.compareAndSet(JobStatus.SCHEDULED, JobStatus.CANCELLED);
    }

    public void markRunning() {
        status.set(JobStatus.RUNNING);
    }

    public void markCompleted() {
        status.set(JobStatus.COMPLETED);
    }

    public void markFailed() {
        status.set(JobStatus.FAILED);
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long remainingMillis = executionTimeMillis - System.currentTimeMillis();
        return unit.convert(remainingMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        if (other == this) {
            return 0;
        }
        if (other instanceof ScheduledTask otherTask) {
            int timeCompare = Long.compare(this.executionTimeMillis, otherTask.executionTimeMillis);
            if (timeCompare != 0) {
                return timeCompare;
            }
            return Long.compare(this.sequence, otherTask.sequence);
        }
        return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
    }
}
