package observer;

public class SmsChannel implements NotificationChannel {
    @Override
    public void send(String userId, String renderedMessage) {
        System.out.println("[SMS -> " + userId + "] " + renderedMessage);
    }
}
