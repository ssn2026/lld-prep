package services;

import adapter.ExternalMarketFeedAdapter;
import adapter.PriceFeed;
import decorator.BrokerageFeeDecorator;
import decorator.ChargeCalculator;
import decorator.GstDecorator;
import decorator.NoCharge;
import decorator.SttDecorator;
import exceptions.IllegalOrderStateException;
import exceptions.InsufficientFundsException;
import exceptions.InsufficientHoldingsException;
import exceptions.InvalidOrderException;
import exceptions.NoQuoteAvailableException;
import external.ExternalMarketFeed;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import model.Account;
import model.Order;
import model.OrderSide;
import model.OrderStatus;
import model.OrderType;
import repository.AccountRepository;
import repository.OrderRepository;
import strategy.LimitOrderStrategy;
import strategy.MarketOrderStrategy;
import strategy.OrderExecutionStrategy;

/**
 * Single public entry point for the stock broker. All caller-facing
 * operations (accounts, price ticks, orders, portfolio/history) go through
 * this class; model/strategy/decorator/adapter/repository classes are
 * internal collaborators.
 */
public class StockBrokerService {

    private final AccountRepository accountRepository = new AccountRepository();
    private final OrderRepository orderRepository = new OrderRepository();
    private final ExternalMarketFeed externalMarketFeed = new ExternalMarketFeed();
    private final PriceFeed priceFeed = new ExternalMarketFeedAdapter(externalMarketFeed);
    private final ChargeCalculator chargeCalculator =
            new GstDecorator(new SttDecorator(new BrokerageFeeDecorator(new NoCharge())));
    private final AtomicInteger orderSequence = new AtomicInteger(1);

    public void openAccount(String accountId, double initialCash) {
        accountRepository.save(new Account(accountId, initialCash));
    }

    /**
     * Simulates a new market tick arriving from the external feed, then
     * rechecks every PENDING order for that symbol against the fresh price
     * -- a limit order that couldn't execute at placement time may now
     * qualify. An order that still can't afford to execute (funds/holdings
     * changed since it was placed) is skipped, not failed, so one order's
     * shortfall doesn't block others from being checked.
     */
    public void publishPriceUpdate(String symbol, double price) {
        externalMarketFeed.publishQuote(symbol, price);
        double currentPrice = priceFeed.getCurrentPrice(symbol);
        for (Order pending : orderRepository.findPendingBySymbol(symbol)) {
            OrderExecutionStrategy strategy = resolveStrategy(pending.getType());
            if (strategy.isExecutable(pending, currentPrice)) {
                try {
                    executeOrder(pending, currentPrice);
                } catch (InsufficientFundsException | InsufficientHoldingsException e) {
                    // leave it pending; the account can't afford it right now
                }
            }
        }
    }

    /**
     * A market order either executes immediately or is rejected outright
     * (no price, no funds, no holdings) -- it is never queued. A limit
     * order that isn't immediately executable is saved as PENDING and
     * rechecked on future price updates.
     */
    public Order placeOrder(String accountId, String symbol, OrderSide side, OrderType type,
            int quantity, Double limitPrice) {
        accountRepository.findByAccountId(accountId);
        if (quantity <= 0) {
            throw new InvalidOrderException("Quantity must be positive");
        }
        if (type == OrderType.LIMIT && limitPrice == null) {
            throw new InvalidOrderException("Limit orders require a limit price");
        }

        Order order = new Order("O" + orderSequence.getAndIncrement(), accountId, symbol, side, type,
                quantity, type == OrderType.LIMIT ? limitPrice : null);

        OrderExecutionStrategy strategy = resolveStrategy(type);
        Double currentPrice = tryGetCurrentPrice(symbol);
        if (currentPrice != null && strategy.isExecutable(order, currentPrice)) {
            executeOrder(order, currentPrice);
        } else if (type == OrderType.MARKET) {
            throw new NoQuoteAvailableException("No market price available for " + symbol);
        }

        orderRepository.save(order);
        return order;
    }

    public void cancelOrder(String orderId) {
        Order order = orderRepository.findByOrderId(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalOrderStateException("Cannot cancel order in status " + order.getStatus());
        }
        order.markCancelled();
    }

    public Account getPortfolio(String accountId) {
        return accountRepository.findByAccountId(accountId);
    }

    public List<Order> getOrderHistory(String accountId) {
        return orderRepository.findByAccountId(accountId);
    }

    private void executeOrder(Order order, double currentPrice) {
        Account account = accountRepository.findByAccountId(order.getAccountId());
        double tradeValue = currentPrice * order.getQuantity();
        double charges = chargeCalculator.totalCharges(tradeValue);

        if (order.getSide() == OrderSide.BUY) {
            double totalCost = tradeValue + charges;
            if (totalCost > account.getCashBalance()) {
                throw new InsufficientFundsException(
                        "Insufficient funds: need " + totalCost + ", available " + account.getCashBalance());
            }
            account.debit(totalCost);
            account.addHolding(order.getSymbol(), order.getQuantity());
        } else {
            if (account.getHoldingQuantity(order.getSymbol()) < order.getQuantity()) {
                throw new InsufficientHoldingsException("Insufficient holdings of " + order.getSymbol()
                        + ": requested " + order.getQuantity()
                        + ", held " + account.getHoldingQuantity(order.getSymbol()));
            }
            account.removeHolding(order.getSymbol(), order.getQuantity());
            account.credit(tradeValue - charges);
        }

        order.markExecuted(currentPrice, charges);
    }

    private Double tryGetCurrentPrice(String symbol) {
        try {
            return priceFeed.getCurrentPrice(symbol);
        } catch (NoQuoteAvailableException e) {
            return null;
        }
    }

    private OrderExecutionStrategy resolveStrategy(OrderType type) {
        return switch (type) {
            case MARKET -> new MarketOrderStrategy();
            case LIMIT -> new LimitOrderStrategy();
        };
    }
}
