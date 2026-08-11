package decorator;

/**
 * In reality GST applies specifically to the brokerage fee, not the whole
 * trade value; approximated here as a small flat rate on trade value to
 * keep every decorator's contract the same (tradeValue in, its own cut
 * out) -- flagged in the README as a known simplification.
 */
public class GstDecorator extends ChargeDecorator {

    private static final double RATE = 0.0005;

    public GstDecorator(ChargeCalculator inner) {
        super(inner);
    }

    @Override
    public double totalCharges(double tradeValue) {
        return inner.totalCharges(tradeValue) + tradeValue * RATE;
    }
}
