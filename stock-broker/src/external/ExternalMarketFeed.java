package external;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stand-in for a third-party market-data vendor's client library: its own
 * method names/types (fetchQuote/ExternalQuote), not designed around this
 * broker's internal vocabulary. StockBrokerService never talks to this
 * class directly -- only adapter.ExternalMarketFeedAdapter does.
 */
public class ExternalMarketFeed {

    private final Map<String, ExternalQuote> latestByTicker = new LinkedHashMap<>();

    public void publishQuote(String ticker, double lastTradedPrice) {
        latestByTicker.put(ticker, new ExternalQuote(ticker, lastTradedPrice));
    }

    /** Returns null if this ticker has never had a quote published -- mirrors a real feed's shape. */
    public ExternalQuote fetchQuote(String ticker) {
        return latestByTicker.get(ticker);
    }
}
