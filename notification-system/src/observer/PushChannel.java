package observer;

public class PushChannel implements NotificationChannel {
    @Override
    public void send(String userId, String renderedMessage) {
        System.out.println("[PUSH -> " + userId + "] " + renderedMessage);
    }
}
