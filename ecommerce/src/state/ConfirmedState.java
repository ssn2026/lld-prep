package state;

import model.OrderStatus;

public final class ConfirmedState implements OrderState {

    public static final ConfirmedState INSTANCE = new ConfirmedState();

    private ConfirmedState() {
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.CONFIRMED;
    }

    @Override
    public OrderState ship() {
        return ShippedState.INSTANCE;
    }

    @Override
    public OrderState cancel() {
        return CancelledState.INSTANCE;
    }
}
