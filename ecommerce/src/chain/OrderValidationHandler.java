package chain;

/**
 * Chain of Responsibility: checkout runs the cart through a pipeline of
 * independent checks (stock, price/coupon, payment) where any link can
 * reject the whole checkout by throwing. EcommerceService only wires the
 * chain together once and calls handle() on the head — it doesn't know or
 * care how many checks there are or what order-specific state each one
 * needs, so a new check (e.g. fraud screening) is a new handler class, not
 * an edit to checkout()'s logic.
 */
public abstract class OrderValidationHandler {

    private OrderValidationHandler next;

    public OrderValidationHandler linkWith(OrderValidationHandler next) {
        this.next = next;
        return next;
    }

    public final void handle(OrderValidationContext context) {
        validate(context);
        if (next != null) {
            next.handle(context);
        }
    }

    protected abstract void validate(OrderValidationContext context);
}
