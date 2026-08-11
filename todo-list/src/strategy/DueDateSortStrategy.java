package strategy;

import java.util.Comparator;
import java.util.List;
import model.Task;

public class DueDateSortStrategy implements TaskSortStrategy {
    @Override
    public List<Task> sort(List<Task> tasks) {
        return tasks.stream()
                .sorted(Comparator.comparingInt(Task::getDueInDays))
                .toList();
    }
}
