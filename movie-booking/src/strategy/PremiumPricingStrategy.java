package strategy;

public class PremiumPricingStrategy implements SeatPricingStrategy {

    private static final double MULTIPLIER = 1.5;

    @Override
    public double calculatePrice(double baseSeatPrice) {
        return baseSeatPrice * MULTIPLIER;
    }
}
