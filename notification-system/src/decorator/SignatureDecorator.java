package decorator;

public class SignatureDecorator implements NotificationContent {
    private final NotificationContent inner;

    public SignatureDecorator(NotificationContent inner) {
        this.inner = inner;
    }

    @Override
    public String render() {
        return inner.render() + "\n-- Sent by NotifyService";
    }
}
