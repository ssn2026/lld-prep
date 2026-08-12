package state;

import model.GameStatus;

/** Terminal: requireInProgress() falls through to GameState's throwing default. */
public class XWonState implements GameState {
    public static final XWonState INSTANCE = new XWonState();

    private XWonState() {
    }

    @Override
    public GameStatus getStatus() {
        return GameStatus.X_WON;
    }
}
