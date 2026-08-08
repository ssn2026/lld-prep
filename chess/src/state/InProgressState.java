package state;

import model.GameStatus;

public final class InProgressState implements GameState {

    public static final InProgressState INSTANCE = new InProgressState();

    private InProgressState() {
    }

    @Override
    public GameStatus getStatus() {
        return GameStatus.IN_PROGRESS;
    }

    @Override
    public boolean allowsMove() {
        return true;
    }
}
