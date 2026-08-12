package decorator;

/** Always adds its prefix when applied -- the SERVICE decides whether to wrap with this, not the decorator itself. */
public class UrgentPrefixDecorator implements NotificationContent {
    private final NotificationContent inner;

    public UrgentPrefixDecorator(NotificationContent inner) {
        this.inner = inner;
    }

    @Override
    public String render() {
        return "*** URGENT *** " + inner.render();
    }
}
