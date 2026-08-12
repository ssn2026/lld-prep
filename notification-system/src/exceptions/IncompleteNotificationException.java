package exceptions;

public class IncompleteNotificationException extends RuntimeException {
    public IncompleteNotificationException(String missingField) {
        super("Cannot build a Notification without a " + missingField);
    }
}
