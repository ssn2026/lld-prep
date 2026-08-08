package strategy;

public class ReclinerPricingStrategy implements SeatPricingStrategy {

    private static final double MULTIPLIER = 2.0;

    @Override
    public double calculatePrice(double baseSeatPrice) {
        return baseSeatPrice * MULTIPLIER;
    }
}
