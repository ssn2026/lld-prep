package observer;

public interface NotificationChannel {
    void send(String userId, String renderedMessage);
}
