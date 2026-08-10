package state;

import model.AtmStatus;

public final class CardInsertedState implements AtmState {

    public static final CardInsertedState INSTANCE = new CardInsertedState();

    private CardInsertedState() {
    }

    @Override
    public AtmStatus getStatus() {
        return AtmStatus.CARD_INSERTED;
    }

    @Override
    public void requireCardInserted() {
    }

    @Override
    public AtmState authenticate() {
        return AuthenticatedState.INSTANCE;
    }

    @Override
    public AtmState ejectCard() {
        return IdleState.INSTANCE;
    }

    @Override
    public AtmState retainCard() {
        return CardRetainedState.INSTANCE;
    }
}
