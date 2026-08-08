package state;

import model.GameStatus;

public final class StalemateState implements GameState {

    public static final StalemateState INSTANCE = new StalemateState();

    private StalemateState() {
    }

    @Override
    public GameStatus getStatus() {
        return GameStatus.STALEMATE;
    }

    @Override
    public boolean allowsMove() {
        return false;
    }
}
