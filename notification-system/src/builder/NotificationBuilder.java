package builder;

import exceptions.IncompleteNotificationException;
import model.Notification;
import model.NotificationPriority;

/** Fluent Builder: Notification has no public constructor, only this. */
public class NotificationBuilder {
    private String title;
    private String body;
    private NotificationPriority priority = NotificationPriority.NORMAL;

    public NotificationBuilder title(String title) {
        this.title = title;
        return this;
    }

    public NotificationBuilder body(String body) {
        this.body = body;
        return this;
    }

    public NotificationBuilder priority(NotificationPriority priority) {
        this.priority = priority;
        return this;
    }

    public Notification build() {
        if (title == null || title.isBlank()) {
            throw new IncompleteNotificationException("title");
        }
        if (body == null || body.isBlank()) {
            throw new IncompleteNotificationException("body");
        }
        return new Notification(title, body, priority);
    }
}
