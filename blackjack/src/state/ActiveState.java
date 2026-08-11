package state;

import model.Card;
import model.Hand;
import model.HandStatus;

public class ActiveState implements HandState {
    public static final ActiveState INSTANCE = new ActiveState();

    private ActiveState() {
    }

    @Override
    public HandStatus getStatus() {
        return HandStatus.ACTIVE;
    }

    @Override
    public void requireActive() {
        // no-op: legal
    }

    @Override
    public HandState hit(Hand hand, Card newCard) {
        hand.addCard(newCard);
        return hand.getTotal() > 21 ? BustedState.INSTANCE : ActiveState.INSTANCE;
    }

    @Override
    public HandState stand(Hand hand) {
        return StandingState.INSTANCE;
    }
}
