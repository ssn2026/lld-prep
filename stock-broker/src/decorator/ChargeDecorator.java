package decorator;

public abstract class ChargeDecorator implements ChargeCalculator {

    protected final ChargeCalculator inner;

    protected ChargeDecorator(ChargeCalculator inner) {
        this.inner = inner;
    }
}
