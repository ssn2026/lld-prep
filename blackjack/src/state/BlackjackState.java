package state;

import model.HandStatus;

/** Terminal: hit()/stand() fall through to HandState's throwing defaults. */
public class BlackjackState implements HandState {
    public static final BlackjackState INSTANCE = new BlackjackState();

    private BlackjackState() {
    }

    @Override
    public HandStatus getStatus() {
        return HandStatus.BLACKJACK;
    }
}
