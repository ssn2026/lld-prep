package strategy;

import model.Order;

/**
 * Strategy pattern: whether an order is executable at the current price
 * depends entirely on its order type. StockBrokerService picks the right
 * strategy per Order.getType() and calls isExecutable() uniformly -- both
 * when placing a new order and when rechecking a PENDING order on every
 * price update -- without ever branching on MARKET vs LIMIT itself.
 */
public interface OrderExecutionStrategy {
    boolean isExecutable(Order order, double currentPrice);
}
