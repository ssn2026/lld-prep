package state;

import model.MatchStatus;

public class Innings1State implements MatchState {
    public static final Innings1State INSTANCE = new Innings1State();

    private Innings1State() {
    }

    @Override
    public MatchStatus getStatus() {
        return MatchStatus.INNINGS_1;
    }

    @Override
    public void requireInningsInProgress() {
        // no-op: legal
    }

    @Override
    public MatchState endInnings() {
        return InningsBreakState.INSTANCE;
    }
}
