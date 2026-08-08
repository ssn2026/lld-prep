package state;

import model.GameStatus;

public final class CheckState implements GameState {

    public static final CheckState INSTANCE = new CheckState();

    private CheckState() {
    }

    @Override
    public GameStatus getStatus() {
        return GameStatus.CHECK;
    }

    @Override
    public boolean allowsMove() {
        return true;
    }
}
