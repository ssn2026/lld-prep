package external;

/**
 * Shape as returned by the (simulated) third-party market-data provider --
 * deliberately named differently from the broker's own vocabulary
 * (ticker/lastTradedPrice instead of symbol/price) to make
 * adapter.ExternalMarketFeedAdapter's translation meaningful rather than a
 * no-op wrapper.
 */
public class ExternalQuote {

    private final String ticker;
    private final double lastTradedPrice;

    public ExternalQuote(String ticker, double lastTradedPrice) {
        this.ticker = ticker;
        this.lastTradedPrice = lastTradedPrice;
    }

    public String getTicker() {
        return ticker;
    }

    public double getLastTradedPrice() {
        return lastTradedPrice;
    }
}
