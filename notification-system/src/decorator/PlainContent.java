package decorator;

import model.Notification;

/** The undecorated base: just the notification's own title/body/priority. */
public class PlainContent implements NotificationContent {
    private final Notification notification;

    public PlainContent(Notification notification) {
        this.notification = notification;
    }

    @Override
    public String render() {
        return "[" + notification.getPriority() + "] " + notification.getTitle() + ": " + notification.getBody();
    }
}
