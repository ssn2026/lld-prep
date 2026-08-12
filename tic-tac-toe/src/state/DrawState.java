package state;

import model.GameStatus;

/** Terminal: requireInProgress() falls through to GameState's throwing default. */
public class DrawState implements GameState {
    public static final DrawState INSTANCE = new DrawState();

    private DrawState() {
    }

    @Override
    public GameStatus getStatus() {
        return GameStatus.DRAW;
    }
}
