package model;

import exceptions.EmptyDeckException;
import java.util.ArrayDeque;
import java.util.Deque;

public class Deck {
    private final Deque<Card> cards;

    public Deck(Deque<Card> cards) {
        this.cards = cards;
    }

    public Card draw() {
        Card card = cards.pollFirst();
        if (card == null) {
            throw new EmptyDeckException();
        }
        return card;
    }

    public int remaining() {
        return cards.size();
    }
}
