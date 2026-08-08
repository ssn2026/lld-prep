package factory;

import model.SeatType;
import strategy.PremiumPricingStrategy;
import strategy.ReclinerPricingStrategy;
import strategy.RegularPricingStrategy;
import strategy.SeatPricingStrategy;

public class SeatPricingStrategyFactory {

    private static final SeatPricingStrategy REGULAR = new RegularPricingStrategy();
    private static final SeatPricingStrategy PREMIUM = new PremiumPricingStrategy();
    private static final SeatPricingStrategy RECLINER = new ReclinerPricingStrategy();

    private SeatPricingStrategyFactory() {
    }

    public static SeatPricingStrategy getStrategy(SeatType seatType) {
        return switch (seatType) {
            case REGULAR -> REGULAR;
            case PREMIUM -> PREMIUM;
            case RECLINER -> RECLINER;
        };
    }
}
