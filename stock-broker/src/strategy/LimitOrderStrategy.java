package strategy;

import model.Order;
import model.OrderSide;

/**
 * A buy limit order executes once the price has dropped to or below the
 * limit (willing to pay at most X); a sell limit order executes once the
 * price has risen to or above the limit (willing to sell for at least X).
 */
public class LimitOrderStrategy implements OrderExecutionStrategy {
    @Override
    public boolean isExecutable(Order order, double currentPrice) {
        if (order.getSide() == OrderSide.BUY) {
            return currentPrice <= order.getLimitPrice();
        }
        return currentPrice >= order.getLimitPrice();
    }
}
