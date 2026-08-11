package factory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import model.Card;
import model.Deck;
import model.Rank;
import model.Suit;

/**
 * Builds a full 52-card deck and shuffles it, keeping the card-construction
 * loop out of Deck itself (same role as parking-lot/'s ParkingSpotFactory).
 * Shuffled with a fixed seed by default so test runs are reproducible --
 * a real table would want a fresh, unseeded shuffle every round.
 */
public class DeckFactory {
    private static final long DEFAULT_SEED = 7L;

    public static Deck createShuffledDeck() {
        return createShuffledDeck(DEFAULT_SEED);
    }

    public static Deck createShuffledDeck(long seed) {
        List<Card> cards = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(cards, new java.util.Random(seed));
        Deque<Card> deque = new ArrayDeque<>(cards);
        return new Deck(deque);
    }
}
