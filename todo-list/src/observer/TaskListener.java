package observer;

import model.TaskStatus;

public interface TaskListener {
    void onStatusChanged(String taskId, TaskStatus from, TaskStatus to);
}
