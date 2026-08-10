package builder;

import java.util.ArrayList;
import java.util.List;
import model.Order;
import model.OrderItem;
import model.Product;

/**
 * Builder pattern: checkout() adds one OrderItem per cart line as it
 * iterates and reserves stock, then calls build() once everything succeeded.
 * That incremental assembly (with a required-fields check at the end) is
 * awkward to express as a single constructor call, since the item list is
 * only fully known after iterating the cart line by line.
 */
public class OrderBuilder {

    private String orderId;
    private String customerId;
    private final List<OrderItem> items = new ArrayList<>();
    private String shippingAddress;
    private double totalAmount;
    private String couponCode;

    public OrderBuilder orderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public OrderBuilder customerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public OrderBuilder addItem(Product product, int quantity, double priceAtPurchase) {
        items.add(new OrderItem(product, quantity, priceAtPurchase));
        return this;
    }

    public OrderBuilder shippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
        return this;
    }

    public OrderBuilder totalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
        return this;
    }

    public OrderBuilder couponCode(String couponCode) {
        this.couponCode = couponCode;
        return this;
    }

    public Order build() {
        if (orderId == null || customerId == null || shippingAddress == null) {
            throw new IllegalStateException("orderId, customerId, and shippingAddress are required");
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("Order must contain at least one item");
        }
        return new Order(orderId, customerId, items, shippingAddress, totalAmount, couponCode);
    }
}
