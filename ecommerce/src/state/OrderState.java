package state;

import exceptions.InvalidOrderStateException;
import model.OrderStatus;

/**
 * State pattern: which OrderStatus an Order is in gates which transitions are
 * legal. Each concrete state overrides only the transitions it allows;
 * everything else falls through to these defaults and throws, so adding a
 * new status means writing one small class instead of extending a
 * switch/if-chain that every transition method would otherwise share.
 */
public interface OrderState {

    OrderStatus getStatus();

    default OrderState confirm() {
        throw new InvalidOrderStateException("Cannot confirm order in status " + getStatus());
    }

    default OrderState ship() {
        throw new InvalidOrderStateException("Cannot ship order in status " + getStatus());
    }

    default OrderState deliver() {
        throw new InvalidOrderStateException("Cannot deliver order in status " + getStatus());
    }

    default OrderState cancel() {
        throw new InvalidOrderStateException("Cannot cancel order in status " + getStatus());
    }
}
