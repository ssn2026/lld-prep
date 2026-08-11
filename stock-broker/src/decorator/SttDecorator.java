package decorator;

/** Securities Transaction Tax -- modeled here as a flat rate on every trade. */
public class SttDecorator extends ChargeDecorator {

    private static final double RATE = 0.001;

    public SttDecorator(ChargeCalculator inner) {
        super(inner);
    }

    @Override
    public double totalCharges(double tradeValue) {
        return inner.totalCharges(tradeValue) + tradeValue * RATE;
    }
}
