package state;

import exceptions.IllegalAtmOperationException;
import model.AtmStatus;

/**
 * State pattern: which AtmStatus the machine is in gates both which
 * transitions are legal (insertCard/authenticate/ejectCard/retainCard) and
 * which non-transitioning operations are legal (require*() guards used
 * before PIN entry / balance / withdraw / deposit / statement). Each
 * concrete state overrides only what it allows; everything else falls
 * through to these defaults and throws, so AtmService never branches on
 * AtmStatus itself -- it just calls the guard or transition and lets the
 * current state decide.
 */
public interface AtmState {

    AtmStatus getStatus();

    default void requireIdle() {
        throw new IllegalAtmOperationException("Requires an idle machine; currently " + getStatus());
    }

    default void requireCardInserted() {
        throw new IllegalAtmOperationException("Requires a card to be inserted; currently " + getStatus());
    }

    default void requireAuthenticated() {
        throw new IllegalAtmOperationException("Requires an authenticated session; currently " + getStatus());
    }

    default AtmState insertCard() {
        throw new IllegalAtmOperationException("Cannot insert card while " + getStatus());
    }

    default AtmState authenticate() {
        throw new IllegalAtmOperationException("Cannot authenticate while " + getStatus());
    }

    default AtmState ejectCard() {
        throw new IllegalAtmOperationException("Cannot eject card while " + getStatus());
    }

    default AtmState retainCard() {
        throw new IllegalAtmOperationException("Cannot retain card while " + getStatus());
    }
}
