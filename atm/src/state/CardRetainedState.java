package state;

import model.AtmStatus;

/**
 * Terminal for the session: every guard/transition falls through to the
 * interface default and throws. Only AtmService.resetMachine() -- an
 * administrative override modeling a technician clearing the machine, not a
 * state transition -- can recover from here.
 */
public final class CardRetainedState implements AtmState {

    public static final CardRetainedState INSTANCE = new CardRetainedState();

    private CardRetainedState() {
    }

    @Override
    public AtmStatus getStatus() {
        return AtmStatus.CARD_RETAINED;
    }
}
