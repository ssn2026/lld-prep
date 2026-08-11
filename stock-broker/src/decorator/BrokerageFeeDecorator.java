package decorator;

public class BrokerageFeeDecorator extends ChargeDecorator {

    private static final double RATE = 0.0025;

    public BrokerageFeeDecorator(ChargeCalculator inner) {
        super(inner);
    }

    @Override
    public double totalCharges(double tradeValue) {
        return inner.totalCharges(tradeValue) + tradeValue * RATE;
    }
}
