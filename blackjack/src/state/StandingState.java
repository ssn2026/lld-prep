package state;

import model.HandStatus;

/** Terminal: hit()/stand() fall through to HandState's throwing defaults. */
public class StandingState implements HandState {
    public static final StandingState INSTANCE = new StandingState();

    private StandingState() {
    }

    @Override
    public HandStatus getStatus() {
        return HandStatus.STANDING;
    }
}
