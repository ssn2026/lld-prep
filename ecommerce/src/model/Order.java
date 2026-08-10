package model;

import java.util.List;
import state.OrderState;
import state.PlacedState;

/**
 * Lifecycle transitions (confirm/ship/deliver/cancel) delegate to the current
 * OrderState (State pattern) rather than switching on an enum here, so each
 * status's legal next-moves live with that status instead of in a big
 * conditional this class would otherwise need to grow for every new status.
 */
public class Order {

    private final String orderId;
    private final String customerId;
    private final List<OrderItem> items;
    private final String shippingAddress;
    private final double totalAmount;
    private final String couponCode;
    private OrderState state = PlacedState.INSTANCE;

    public Order(String orderId, String customerId, List<OrderItem> items,
            String shippingAddress, double totalAmount, String couponCode) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = List.copyOf(items);
        this.shippingAddress = shippingAddress;
        this.totalAmount = totalAmount;
        this.couponCode = couponCode;
    }

    public void confirm() {
        state = state.confirm();
    }

    public void ship() {
        state = state.ship();
    }

    public void deliver() {
        state = state.deliver();
    }

    public void cancel() {
        state = state.cancel();
    }

    public OrderStatus getStatus() {
        return state.getStatus();
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getCouponCode() {
        return couponCode;
    }
}
