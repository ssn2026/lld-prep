package services;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import model.Task;
import model.TaskPriority;
import model.TaskStatus;
import observer.TaskListener;
import repository.TaskRepository;
import strategy.TaskSortStrategy;

/**
 * The single public entry point. Owns task creation/lookup (via
 * TaskRepository), lifecycle transitions (delegated to each Task's own
 * TaskState), listing (delegated to a pluggable TaskSortStrategy), and
 * notifies TaskListeners on every status change.
 */
public class TodoListService {
    private final TaskRepository repository = new TaskRepository();
    private final List<TaskListener> listeners = new java.util.ArrayList<>();
    private final AtomicInteger idSeq = new AtomicInteger();
    private final AtomicInteger createdOrderSeq = new AtomicInteger();

    public void addListener(TaskListener listener) {
        listeners.add(listener);
    }

    public String addTask(String title, String description, TaskPriority priority, int dueInDays) {
        String taskId = "T" + idSeq.incrementAndGet();
        Task task = new Task(taskId, title, description, priority, dueInDays, createdOrderSeq.incrementAndGet());
        repository.save(task);
        return taskId;
    }

    public void startTask(String taskId) {
        transition(taskId, Task::start);
    }

    public void completeTask(String taskId) {
        transition(taskId, Task::complete);
    }

    public void reopenTask(String taskId) {
        transition(taskId, Task::reopen);
    }

    public void archiveTask(String taskId) {
        transition(taskId, Task::archive);
    }

    public void deleteTask(String taskId) {
        repository.delete(taskId);
    }

    public Task getTask(String taskId) {
        return repository.findById(taskId);
    }

    public List<Task> listTasks(TaskSortStrategy sortStrategy) {
        return sortStrategy.sort(repository.findAll());
    }

    private void transition(String taskId, java.util.function.Consumer<Task> mutation) {
        Task task = repository.findById(taskId);
        TaskStatus before = task.getStatus();
        mutation.accept(task);
        TaskStatus after = task.getStatus();
        if (before != after) {
            notifyStatusChanged(taskId, before, after);
        }
    }

    private void notifyStatusChanged(String taskId, TaskStatus from, TaskStatus to) {
        for (TaskListener listener : listeners) {
            listener.onStatusChanged(taskId, from, to);
        }
    }
}
