package state;

import model.OrderStatus;

/**
 * Terminal state: every transition falls through to the interface default
 * and throws InvalidOrderStateException.
 */
public final class DeliveredState implements OrderState {

    public static final DeliveredState INSTANCE = new DeliveredState();

    private DeliveredState() {
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.DELIVERED;
    }
}
