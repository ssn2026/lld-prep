package strategy;

import java.util.Random;

/** Seeded so test runs are reproducible -- same simplification as blackjack/'s DeckFactory. */
public class RandomDiceStrategy implements DiceStrategy {
    private final Random random;

    public RandomDiceStrategy(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public int roll() {
        return random.nextInt(6) + 1;
    }
}
