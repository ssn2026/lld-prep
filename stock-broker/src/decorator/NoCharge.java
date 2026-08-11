package decorator;

/** The core component every fee decorator ultimately wraps: zero charges on its own. */
public class NoCharge implements ChargeCalculator {
    @Override
    public double totalCharges(double tradeValue) {
        return 0.0;
    }
}
