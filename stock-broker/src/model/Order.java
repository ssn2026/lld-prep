package model;

public class Order {

    private final String orderId;
    private final String accountId;
    private final String symbol;
    private final OrderSide side;
    private final OrderType type;
    private final int quantity;
    private final Double limitPrice;
    private OrderStatus status = OrderStatus.PENDING;
    private Double executionPrice;
    private Double charges;

    public Order(String orderId, String accountId, String symbol, OrderSide side, OrderType type,
            int quantity, Double limitPrice) {
        this.orderId = orderId;
        this.accountId = accountId;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.quantity = quantity;
        this.limitPrice = limitPrice;
    }

    public void markExecuted(double executionPrice, double charges) {
        this.executionPrice = executionPrice;
        this.charges = charges;
        this.status = OrderStatus.EXECUTED;
    }

    public void markCancelled() {
        this.status = OrderStatus.CANCELLED;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getSymbol() {
        return symbol;
    }

    public OrderSide getSide() {
        return side;
    }

    public OrderType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public Double getLimitPrice() {
        return limitPrice;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Double getExecutionPrice() {
        return executionPrice;
    }

    public Double getCharges() {
        return charges;
    }
}
