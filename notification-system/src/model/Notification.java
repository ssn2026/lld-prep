package model;

/** Immutable; only ever constructed via builder.NotificationBuilder. */
public class Notification {
    private final String title;
    private final String body;
    private final NotificationPriority priority;

    public Notification(String title, String body, NotificationPriority priority) {
        this.title = title;
        this.body = body;
        this.priority = priority;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public NotificationPriority getPriority() {
        return priority;
    }
}
