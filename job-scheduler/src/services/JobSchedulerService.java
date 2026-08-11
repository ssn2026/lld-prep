package services;

import exceptions.DuplicateJobIdException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import model.Job;
import model.JobStatus;
import model.ScheduledTask;
import observer.JobListener;
import repository.JobRegistry;
import strategy.Trigger;

/**
 * The single public entry point for the job scheduler library. Internally
 * owns:
 *  - a DelayQueue that always hands back the next-due task first and blocks
 *    the dispatcher thread until it's actually due (no manual polling loop)
 *  - a JobRegistry (jobId -> ScheduledTask) for O(1) cancellation/status
 *  - a fixed worker thread pool that actually runs due jobs, so one slow
 *    job can't block the dispatcher from picking up the next one
 *  - one dispatcher thread whose only job is: wait for the next due task,
 *    hand it to a worker, repeat
 */
public class JobSchedulerService {
    private final DelayQueue<ScheduledTask> delayQueue = new DelayQueue<>();
    private final JobRegistry registry = new JobRegistry();
    private final ExecutorService workerPool;
    private final Thread dispatcherThread;
    private final CopyOnWriteArrayList<JobListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean running = true;

    public JobSchedulerService(int workerThreads) {
        this.workerPool = Executors.newFixedThreadPool(workerThreads);
        this.dispatcherThread = new Thread(this::dispatchLoop, "job-dispatcher");
        this.dispatcherThread.setDaemon(true);
        this.dispatcherThread.start();
    }

    public void addListener(JobListener listener) {
        listeners.add(listener);
    }

    public void scheduleJob(String jobId, Job job, Trigger trigger) {
        if (registry.contains(jobId)) {
            throw new DuplicateJobIdException(jobId);
        }
        long executionTime = trigger.firstExecutionTime(System.currentTimeMillis());
        ScheduledTask task = new ScheduledTask(jobId, job, trigger, executionTime);
        registry.register(task);
        delayQueue.put(task);
    }

    /**
     * Best-effort cancel. Returns true if the pending occurrence was
     * actually prevented from running; false if it was already running or
     * finished (a recurring job's *future* occurrences are stopped either
     * way).
     */
    public boolean cancelJob(String jobId) {
        ScheduledTask task = registry.findByJobId(jobId);
        return task.cancel();
    }

    public JobStatus getJobStatus(String jobId) {
        return registry.findByJobId(jobId).getStatus();
    }

    public void shutdown() throws InterruptedException {
        running = false;
        dispatcherThread.interrupt();
        workerPool.shutdown();
        workerPool.awaitTermination(5, TimeUnit.SECONDS);
    }

    private void dispatchLoop() {
        while (running) {
            ScheduledTask task;
            try {
                task = delayQueue.take();
            } catch (InterruptedException e) {
                continue;
            }
            if (task.getStatus() == JobStatus.CANCELLED) {
                continue;
            }
            task.markRunning();
            workerPool.submit(() -> runTask(task));
        }
    }

    private void runTask(ScheduledTask task) {
        try {
            task.getJob().execute();
            task.markCompleted();
            notifyCompleted(task.getJobId());
        } catch (Exception e) {
            task.markFailed();
            notifyFailed(task.getJobId(), e);
        }

        if (task.getTrigger().isRecurring() && !task.isCancelRequested()) {
            long nextTime = task.getTrigger().nextExecutionTime(task.getExecutionTimeMillis());
            ScheduledTask nextTask = task.withNextExecution(nextTime);
            registry.register(nextTask);
            delayQueue.put(nextTask);
        }
    }

    private void notifyCompleted(String jobId) {
        for (JobListener listener : listeners) {
            listener.onJobCompleted(jobId);
        }
    }

    private void notifyFailed(String jobId, Throwable error) {
        for (JobListener listener : listeners) {
            listener.onJobFailed(jobId, error);
        }
    }
}
