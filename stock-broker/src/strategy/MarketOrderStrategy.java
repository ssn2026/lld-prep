package strategy;

import model.Order;

/** A market order executes immediately at whatever the current price is. */
public class MarketOrderStrategy implements OrderExecutionStrategy {
    @Override
    public boolean isExecutable(Order order, double currentPrice) {
        return true;
    }
}
