package state;

import model.MatchStatus;

public class Innings2State implements MatchState {
    public static final Innings2State INSTANCE = new Innings2State();

    private Innings2State() {
    }

    @Override
    public MatchStatus getStatus() {
        return MatchStatus.INNINGS_2;
    }

    @Override
    public void requireInningsInProgress() {
        // no-op: legal
    }

    @Override
    public MatchState endInnings() {
        return CompletedState.INSTANCE;
    }
}
