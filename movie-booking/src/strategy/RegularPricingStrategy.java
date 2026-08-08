package strategy;

public class RegularPricingStrategy implements SeatPricingStrategy {

    @Override
    public double calculatePrice(double baseSeatPrice) {
        return baseSeatPrice;
    }
}
