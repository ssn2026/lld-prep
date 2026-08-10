package state;

import model.AtmStatus;

public final class IdleState implements AtmState {

    public static final IdleState INSTANCE = new IdleState();

    private IdleState() {
    }

    @Override
    public AtmStatus getStatus() {
        return AtmStatus.IDLE;
    }

    @Override
    public void requireIdle() {
    }

    @Override
    public AtmState insertCard() {
        return CardInsertedState.INSTANCE;
    }
}
