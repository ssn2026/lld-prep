package decorator;

/**
 * Decorator pattern: the total charges on a trade are built by stacking
 * independent fee layers (brokerage, STT, GST) on top of a no-charge core,
 * each one adding its own cut of the trade value without knowing about the
 * others. StockBrokerService wires the stack once and calls
 * totalCharges(tradeValue) without caring how many layers are in it --
 * adding a new fee (e.g. a stamp duty) is a new decorator class, not a new
 * branch in the settlement math.
 */
public interface ChargeCalculator {
    double totalCharges(double tradeValue);
}
