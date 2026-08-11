package state;

import exceptions.IllegalHandActionException;
import model.Card;
import model.Hand;
import model.HandStatus;

/**
 * One Hand's own lifecycle, held per-instance on the Hand itself -- same
 * shape as todo-list/'s per-Task TaskState (many independent hands per
 * round), not the ATM/CrickInfo style of one shared state on a service.
 */
public interface HandState {
    HandStatus getStatus();

    default void requireActive() {
        throw new IllegalHandActionException(getStatus(), "hit/stand on");
    }

    /** Only reached once requireActive() has already passed. */
    default HandState hit(Hand hand, Card newCard) {
        throw new IllegalHandActionException(getStatus(), "hit");
    }

    default HandState stand(Hand hand) {
        throw new IllegalHandActionException(getStatus(), "stand");
    }
}
