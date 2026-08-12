package state;

import model.GameStatus;

/** Terminal: requireInProgress() falls through to GameState's throwing default. */
public class OWonState implements GameState {
    public static final OWonState INSTANCE = new OWonState();

    private OWonState() {
    }

    @Override
    public GameStatus getStatus() {
        return GameStatus.O_WON;
    }
}
