package state;

import model.MatchStatus;

public class InningsBreakState implements MatchState {
    public static final InningsBreakState INSTANCE = new InningsBreakState();

    private InningsBreakState() {
    }

    @Override
    public MatchStatus getStatus() {
        return MatchStatus.INNINGS_BREAK;
    }

    @Override
    public MatchState startSecondInnings() {
        return Innings2State.INSTANCE;
    }
}
