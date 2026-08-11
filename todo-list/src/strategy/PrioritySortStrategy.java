package strategy;

import java.util.Comparator;
import java.util.List;
import model.Task;
import model.TaskPriority;

public class PrioritySortStrategy implements TaskSortStrategy {
    @Override
    public List<Task> sort(List<Task> tasks) {
        return tasks.stream()
                .sorted(Comparator.comparingInt((Task t) -> rank(t.getPriority())))
                .toList();
    }

    private int rank(TaskPriority priority) {
        return switch (priority) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }
}
