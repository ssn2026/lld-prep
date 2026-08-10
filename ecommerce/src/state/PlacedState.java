package state;

import model.OrderStatus;

public final class PlacedState implements OrderState {

    public static final PlacedState INSTANCE = new PlacedState();

    private PlacedState() {
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.PLACED;
    }

    @Override
    public OrderState confirm() {
        return ConfirmedState.INSTANCE;
    }

    @Override
    public OrderState cancel() {
        return CancelledState.INSTANCE;
    }
}
