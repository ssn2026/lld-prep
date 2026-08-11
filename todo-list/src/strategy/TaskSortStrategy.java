package strategy;

import java.util.List;
import model.Task;

public interface TaskSortStrategy {
    List<Task> sort(List<Task> tasks);
}
