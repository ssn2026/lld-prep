package repository;

import exceptions.JobNotFoundException;
import java.util.concurrent.ConcurrentHashMap;
import model.ScheduledTask;

/**
 * jobId -> current ScheduledTask, for O(1) lookup/cancellation instead of
 * scanning the scheduler's DelayQueue. ConcurrentHashMap because the
 * dispatcher thread, worker threads, and whichever thread calls
 * scheduleJob()/cancelJob() all touch this at once.
 */
public class JobRegistry {
    private final ConcurrentHashMap<String, ScheduledTask> tasksByJobId = new ConcurrentHashMap<>();

    public void register(ScheduledTask task) {
        tasksByJobId.put(task.getJobId(), task);
    }

    public boolean contains(String jobId) {
        return tasksByJobId.containsKey(jobId);
    }

    public ScheduledTask findByJobId(String jobId) {
        ScheduledTask task = tasksByJobId.get(jobId);
        if (task == null) {
            throw new JobNotFoundException(jobId);
        }
        return task;
    }
}
