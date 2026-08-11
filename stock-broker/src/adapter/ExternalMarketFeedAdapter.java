package adapter;

import exceptions.NoQuoteAvailableException;
import external.ExternalMarketFeed;
import external.ExternalQuote;

public class ExternalMarketFeedAdapter implements PriceFeed {

    private final ExternalMarketFeed externalMarketFeed;

    public ExternalMarketFeedAdapter(ExternalMarketFeed externalMarketFeed) {
        this.externalMarketFeed = externalMarketFeed;
    }

    @Override
    public double getCurrentPrice(String symbol) {
        ExternalQuote quote = externalMarketFeed.fetchQuote(symbol);
        if (quote == null) {
            throw new NoQuoteAvailableException("No market price available for " + symbol);
        }
        return quote.getLastTradedPrice();
    }
}
