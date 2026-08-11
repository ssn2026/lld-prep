package state;

import model.HandStatus;

/** Terminal: hit()/stand() fall through to HandState's throwing defaults. */
public class BustedState implements HandState {
    public static final BustedState INSTANCE = new BustedState();

    private BustedState() {
    }

    @Override
    public HandStatus getStatus() {
        return HandStatus.BUSTED;
    }
}
