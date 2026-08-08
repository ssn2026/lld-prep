package state;

import model.GameStatus;

public final class CheckmateState implements GameState {

    public static final CheckmateState INSTANCE = new CheckmateState();

    private CheckmateState() {
    }

    @Override
    public GameStatus getStatus() {
        return GameStatus.CHECKMATE;
    }

    @Override
    public boolean allowsMove() {
        return false;
    }
}
