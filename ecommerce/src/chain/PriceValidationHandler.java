package chain;

import java.util.Map;
import model.CartItem;
import model.Product;

/**
 * Recomputes the order total from the catalog's current prices rather than
 * trusting any client-supplied amount, and applies the coupon discount (if
 * any) here so the authorized amount downstream in PaymentAuthorizationHandler
 * is always server-derived.
 */
public class PriceValidationHandler extends OrderValidationHandler {

    private static final Map<String, Double> COUPON_DISCOUNTS = Map.of(
            "SAVE10", 0.10,
            "SAVE20", 0.20);

    @Override
    protected void validate(OrderValidationContext context) {
        double subtotal = 0.0;
        for (CartItem item : context.getCart().getItems()) {
            Product product = context.getProductRepository().findBySku(item.getProduct().getSku());
            subtotal += product.getPrice() * item.getQuantity();
        }
        double discount = context.getCouponCode() == null
                ? 0.0
                : COUPON_DISCOUNTS.getOrDefault(context.getCouponCode(), 0.0);
        context.setComputedTotal(subtotal * (1 - discount));
    }
}
