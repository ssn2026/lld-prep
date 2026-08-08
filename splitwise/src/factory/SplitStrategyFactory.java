package factory;

import model.SplitType;
import strategy.EqualSplitStrategy;
import strategy.ExactSplitStrategy;
import strategy.PercentSplitStrategy;
import strategy.SplitStrategy;

public class SplitStrategyFactory {

    private SplitStrategyFactory() {
    }

    public static SplitStrategy getStrategy(SplitType type) {
        return switch (type) {
            case EQUAL -> new EqualSplitStrategy();
            case EXACT -> new ExactSplitStrategy();
            case PERCENT -> new PercentSplitStrategy();
        };
    }
}
