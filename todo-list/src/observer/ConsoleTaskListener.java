package observer;

import model.TaskStatus;

public class ConsoleTaskListener implements TaskListener {
    @Override
    public void onStatusChanged(String taskId, TaskStatus from, TaskStatus to) {
        System.out.println("[listener] " + taskId + ": " + from + " -> " + to);
    }
}
