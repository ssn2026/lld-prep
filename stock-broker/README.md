# Stock Broker

> **Mode: Learning** — this design (happy flow, class breakdown, pattern
> choices) was authored by Claude, not the user. See `CLAUDE.md` → "Mode:
> Learning" for what that implies about ownership of the decisions below.

A single-broker trading system: accounts hold cash and stock positions, a
simulated market feed publishes price ticks, and orders (market or limit)
execute against those ticks with brokerage/tax charges deducted.

## Happy flow

1. Accounts are opened with a starting cash balance
   (`StockBrokerService.openAccount()`).
2. A price tick arrives for a symbol (`publishPriceUpdate()`), routed
   through a simulated third-party feed.
3. A user places an order (`placeOrder()`): a **market** order executes
   immediately at the current price, or is rejected outright if there's no
   price yet or funds/holdings are insufficient — it is never queued. A
   **limit** order executes immediately if its condition is already met,
   otherwise it's saved `PENDING`.
4. Every future price tick for that symbol rechecks all `PENDING` orders
   and executes any that now qualify — an order that still can't be
   afforded is skipped (stays pending), not failed.
5. On execution, the trade value has brokerage + STT + GST charges stacked
   on top (added to cost for a buy, subtracted from proceeds for a sell),
   and the account's cash/holdings update accordingly.
6. Users can cancel a still-`PENDING` order, and view their portfolio
   (`getPortfolio()`) or full order history (`getOrderHistory()`) at any
   time.

## Design patterns used

- **Strategy** — `strategy/OrderExecutionStrategy.java` with
  `MarketOrderStrategy` (always executable) and `LimitOrderStrategy`
  (`price <= limit` for a buy, `price >= limit` for a sell).
  `StockBrokerService` calls `isExecutable()` identically whether it's
  checking a brand-new order or rechecking a `PENDING` one on a price
  update — it never branches on `OrderType` itself.
- **Decorator** — `decorator/ChargeCalculator.java` with a `NoCharge` core
  wrapped by `BrokerageFeeDecorator` → `SttDecorator` → `GstDecorator`,
  each adding its own independent cut of the trade value. The stack is
  built once in `StockBrokerService`'s constructor; adding a new fee (e.g.
  a stamp duty) means adding a decorator class, not touching the
  settlement math in `executeOrder()`.
- **Adapter** — `adapter/PriceFeed.java` (the interface `StockBrokerService`
  and both execution strategies actually depend on) implemented by
  `adapter/ExternalMarketFeedAdapter.java`, which translates
  `external/ExternalMarketFeed.java`'s differently-shaped API
  (`fetchQuote(ticker): ExternalQuote`) into the broker's own vocabulary
  (`getCurrentPrice(symbol): double`). Nothing outside the adapter ever
  touches `ExternalMarketFeed` or `ExternalQuote` directly — swapping in a
  real market-data vendor later means writing one new adapter, not
  touching the broker's core logic.

## Structure

```
stock-broker/
  src/
    model/       Account, Order, OrderSide, OrderType, OrderStatus
    strategy/    OrderExecutionStrategy + Market/LimitOrderStrategy
    decorator/   ChargeCalculator, NoCharge, ChargeDecorator + Brokerage/Stt/GstDecorator
    external/    ExternalMarketFeed, ExternalQuote (simulated third-party vendor)
    adapter/     PriceFeed (target interface), ExternalMarketFeedAdapter
    repository/  AccountRepository, OrderRepository (in-memory)
    exceptions/  AccountNotFoundException, NoQuoteAvailableException, InsufficientFundsException,
                 InsufficientHoldingsException, OrderNotFoundException, InvalidOrderException,
                 IllegalOrderStateException
    services/    StockBrokerService (the only public entry point)
    Main.java    Reads a command script, drives the service, writes a transcript
  test/
    input/scenario.txt   Market + limit orders, price-triggered execution, and every edge case below
    output/output.txt    Captured run transcript
  diagrams/
    generate.py         Data-only script that builds stock-broker.drawio via docs/tooling/drawio_uml.py
    stock-broker.drawio  Class diagram + 2 sequence diagrams (place market order, price-triggered limit order)
  explainer/index.html   Interactive step-through: tap an action to watch a market buy (Strategy +
                          Decorator + Adapter all in one call), two pending limit orders, price ticks
                          that trigger them, and an insufficient-holdings rejection (open directly in
                          a browser)
```

## Running it

```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

Requires the VS Code "Extension Pack for Java" (Microsoft) for the
`.vscode/launch.json` run config to work — open the `stock-broker/`
folder itself as the workspace root, then use the "Run Main
(scenario.txt)" config.

## Known gaps (flagged, not fixed)

- No persistence — all state is in-memory and lost on process exit.
- No concurrency control — `publishPriceUpdate()`'s recheck loop and
  `placeOrder()`'s immediate-execution path both read-then-write account
  state without synchronization.
- GST is modeled as a flat rate on trade value; in reality it applies
  specifically to the brokerage fee, not the whole trade — approximated
  here to keep every `ChargeCalculator` layer's contract identical
  (trade value in, its own cut out).
- A rejected order (market order with no price, or any order failing the
  funds/holdings check at execution time) consumes an order-id sequence
  number but is never saved — it genuinely never existed as far as
  `getOrderHistory()` is concerned, which the test scenario demonstrates
  explicitly (see the `CANCEL O4` line).
- `LimitOrderStrategy` has no expiry (a "good-till-date" concept) — a
  `PENDING` limit order waits forever until it either executes or is
  explicitly cancelled.
- Orders are pinned to whatever price triggered execution — no
  partial fills, no slippage modeling, and every order fills for its
  full quantity or not at all.
