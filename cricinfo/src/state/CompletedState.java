package state;

import model.MatchStatus;

/** Terminal: every transition falls through to MatchState's throwing defaults. */
public class CompletedState implements MatchState {
    public static final CompletedState INSTANCE = new CompletedState();

    private CompletedState() {
    }

    @Override
    public MatchStatus getStatus() {
        return MatchStatus.COMPLETED;
    }
}
