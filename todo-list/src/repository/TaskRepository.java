package repository;

import exceptions.TaskNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Task;

public class TaskRepository {
    private final Map<String, Task> tasksById = new LinkedHashMap<>();

    public void save(Task task) {
        tasksById.put(task.getId(), task);
    }

    public Task findById(String taskId) {
        Task task = tasksById.get(taskId);
        if (task == null) {
            throw new TaskNotFoundException(taskId);
        }
        return task;
    }

    public void delete(String taskId) {
        if (tasksById.remove(taskId) == null) {
            throw new TaskNotFoundException(taskId);
        }
    }

    public List<Task> findAll() {
        return new ArrayList<>(tasksById.values());
    }
}
