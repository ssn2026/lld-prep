package model;

import state.TaskState;
import state.TodoState;

public class Task {
    private final String id;
    private final String title;
    private final String description;
    private final TaskPriority priority;
    private final int dueInDays;
    private final int createdOrder;
    private TaskState state;

    public Task(String id, String title, String description, TaskPriority priority, int dueInDays, int createdOrder) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueInDays = dueInDays;
        this.createdOrder = createdOrder;
        this.state = TodoState.INSTANCE;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public int getDueInDays() {
        return dueInDays;
    }

    public int getCreatedOrder() {
        return createdOrder;
    }

    public TaskStatus getStatus() {
        return state.getStatus();
    }

    public void start() {
        state = state.start(this);
    }

    public void complete() {
        state = state.complete(this);
    }

    public void reopen() {
        state = state.reopen(this);
    }

    public void archive() {
        state = state.archive(this);
    }
}
