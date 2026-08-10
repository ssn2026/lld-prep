package state;

import model.OrderStatus;

/**
 * Terminal state: every transition falls through to the interface default
 * and throws InvalidOrderStateException.
 */
public final class CancelledState implements OrderState {

    public static final CancelledState INSTANCE = new CancelledState();

    private CancelledState() {
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.CANCELLED;
    }
}
