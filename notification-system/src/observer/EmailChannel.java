package observer;

public class EmailChannel implements NotificationChannel {
    @Override
    public void send(String userId, String renderedMessage) {
        System.out.println("[EMAIL -> " + userId + "] " + renderedMessage);
    }
}
