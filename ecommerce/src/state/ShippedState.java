package state;

import model.OrderStatus;

/**
 * No cancel() override: once an order has shipped it's already in transit,
 * so cancellation is no longer offered (only confirm()->ship()->deliver()
 * cancellation windows are). Falls through to the interface default, which
 * throws InvalidOrderStateException.
 */
public final class ShippedState implements OrderState {

    public static final ShippedState INSTANCE = new ShippedState();

    private ShippedState() {
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.SHIPPED;
    }

    @Override
    public OrderState deliver() {
        return DeliveredState.INSTANCE;
    }
}
