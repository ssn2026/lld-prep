package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import state.ActiveState;
import state.BlackjackState;
import state.HandState;

public class Hand {
    private final List<Card> cards = new ArrayList<>();
    private HandState state = ActiveState.INSTANCE;

    public void addCard(Card card) {
        cards.add(card);
    }

    /** Call once, right after the initial two cards are dealt. */
    public void settleInitialState() {
        state = getTotal() == 21 ? BlackjackState.INSTANCE : ActiveState.INSTANCE;
    }

    public int getTotal() {
        int total = 0;
        int aces = 0;
        for (Card card : cards) {
            total += card.getRank().getValue();
            if (card.getRank() == Rank.ACE) {
                aces++;
            }
        }
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return total;
    }

    public HandState getState() {
        return state;
    }

    public void setState(HandState state) {
        this.state = state;
    }

    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }
}
