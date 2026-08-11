package adapter;

/**
 * Adapter pattern target interface: the shape StockBrokerService and the
 * OrderExecutionStrategy family actually depend on. Neither knows or cares
 * that the real data comes from external.ExternalMarketFeed's differently
 * shaped API.
 */
public interface PriceFeed {
    double getCurrentPrice(String symbol);
}
