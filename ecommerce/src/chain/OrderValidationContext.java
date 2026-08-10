package chain;

import model.Cart;
import repository.ProductRepository;

/**
 * Threaded through the validation chain: each handler reads the cart/coupon
 * inputs it needs and, in PriceValidationHandler's case, writes the
 * authoritative computedTotal for the next handler (and the caller,
 * post-chain) to read.
 */
public class OrderValidationContext {

    private final Cart cart;
    private final ProductRepository productRepository;
    private final String couponCode;
    private final String paymentMethod;
    private double computedTotal;

    public OrderValidationContext(Cart cart, ProductRepository productRepository,
            String couponCode, String paymentMethod) {
        this.cart = cart;
        this.productRepository = productRepository;
        this.couponCode = couponCode;
        this.paymentMethod = paymentMethod;
    }

    public Cart getCart() {
        return cart;
    }

    public ProductRepository getProductRepository() {
        return productRepository;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public double getComputedTotal() {
        return computedTotal;
    }

    public void setComputedTotal(double computedTotal) {
        this.computedTotal = computedTotal;
    }
}
