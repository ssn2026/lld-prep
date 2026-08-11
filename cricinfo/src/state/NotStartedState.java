package state;

import model.MatchStatus;

public class NotStartedState implements MatchState {
    public static final NotStartedState INSTANCE = new NotStartedState();

    private NotStartedState() {
    }

    @Override
    public MatchStatus getStatus() {
        return MatchStatus.NOT_STARTED;
    }

    @Override
    public MatchState startFirstInnings() {
        return Innings1State.INSTANCE;
    }
}
