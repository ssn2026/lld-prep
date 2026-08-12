# Stock Broker — Simple Walkthrough

This file explains the whole codebase in plain language, in the order you
should read it. No prior context assumed.

---

## 1. The story in one paragraph

This is a (very simplified) single-broker trading system. A customer opens
an account with some starting cash. Somewhere out in the world, a market
data vendor keeps publishing the latest traded price for each stock symbol
("AAPL is now $150"), and the broker consumes those ticks. A customer then
places an order to buy or sell a symbol, either as a **market order** ("buy
right now, whatever the price is") or a **limit order** ("only buy if the
price drops to $140 or below" / "only sell if it rises to $160 or above").
A market order either executes immediately or is flatly rejected — it never
waits around. A limit order executes immediately if its condition is
*already* satisfied, and otherwise sits as `PENDING`, quietly re-checked
every time a fresh price tick for that symbol arrives, until it either
executes or gets cancelled. Whenever a trade actually executes, the broker
takes its cut — a stack of small percentage fees (brokerage, a securities
transaction tax, and GST) — before updating the account's cash and stock
holdings. Underneath, the design deliberately separates three concerns that
are easy to tangle together: *how* an order decides it's ready to execute,
*how much* it costs to execute, and *where* the live price actually comes
from.

---

## 2. The one door you're allowed to knock on

`src/services/StockBrokerService.java` is the **only** class anything
outside the package is meant to call. Everything else (`model`, `strategy`,
`decorator`, `adapter`, `external`, `repository`, `exceptions`) is a helper
this class uses internally.

| Method | What it does |
|---|---|
| `openAccount(accountId, initialCash)` | Create an account with a starting cash balance |
| `publishPriceUpdate(symbol, price)` | Simulate a new market tick; also rechecks every `PENDING` order for that symbol |
| `placeOrder(accountId, symbol, side, type, quantity, limitPrice)` | Place a market or limit order, get back the `Order` |
| `cancelOrder(orderId)` | Cancel a still-`PENDING` order |
| `getPortfolio(accountId)` | Get the `Account` (cash balance + holdings) |
| `getOrderHistory(accountId)` | Get every order ever placed by an account |

---

## 3. Read the code in this order

### Step 1 — the building blocks (`src/model/`)

- **`OrderSide.java`** — an enum: `BUY`, `SELL`.
- **`OrderType.java`** — an enum: `MARKET`, `LIMIT`.
- **`OrderStatus.java`** — an enum: `PENDING`, `EXECUTED`, `CANCELLED`. An
  order is always in exactly one of these.
- **`Account.java`** — a cash balance (`debit`/`credit` to change it) and a
  `Map<String, Integer>` of symbol → quantity held (`addHolding`/
  `removeHolding`). `removeHolding` deletes the map entry entirely once a
  holding reaches zero, rather than leaving a "0 shares" entry lying
  around — worth noticing since it's the kind of small cleanup detail that's
  easy to skip.
- **`Order.java`** — id, which account placed it, the symbol, `OrderSide`,
  `OrderType`, `quantity`, and (for limit orders only) a `limitPrice`. It
  starts life `PENDING` and has exactly two ways to change: `markExecuted
  (executionPrice, charges)` and `markCancelled()`. Once either has been
  called, nothing in the codebase ever moves the order back to `PENDING`.

**None of these classes decide anything — they're storage, with the mild
exception of `Account`'s holding bookkeeping. The actual decisions —
"is this order ready to execute?" and "how much does executing it cost?" —
live in the next two packages.**

### Step 2 — is this order executable right now? (`src/strategy/`)

- **`OrderExecutionStrategy.java`** — one method:
  `isExecutable(order, currentPrice)`.
- **`MarketOrderStrategy.java`** — always returns `true`. A market order is
  executable the instant there's *any* price to execute it at.
- **`LimitOrderStrategy.java`** — checks the order's side:
  ```java
  if (order.getSide() == OrderSide.BUY) {
      return currentPrice <= order.getLimitPrice();
  }
  return currentPrice >= order.getLimitPrice();
  ```
  In plain words: a buy-limit order is "I'll pay at most $X," so it's ready
  once the price has dropped to $X or below; a sell-limit order is "I'll
  accept at least $X," so it's ready once the price has risen to $X or
  above.

This is the **Strategy pattern**. The payoff shows up in
`StockBrokerService`: the exact same call —
`strategy.isExecutable(order, currentPrice)` — is used both the moment an
order is first placed *and* every time a later price tick rechecks a
`PENDING` order. `StockBrokerService` never writes `if (type == MARKET)`
anywhere; it just asks whichever strategy object matches the order's type.

### Step 3 — how much does executing cost? (`src/decorator/`)

- **`ChargeCalculator.java`** — one method: `totalCharges(tradeValue)`.
- **`NoCharge.java`** — the "core" object at the bottom of the stack;
  always returns `0.0`.
- **`ChargeDecorator.java`** — an abstract base class that every fee layer
  extends. It holds one field, `inner` (another `ChargeCalculator`), set
  through its constructor.
- **`BrokerageFeeDecorator.java`**, **`SttDecorator.java`**,
  **`GstDecorator.java`** — each one wraps an `inner` calculator and adds
  its own percentage cut on top:
  ```java
  // BrokerageFeeDecorator, RATE = 0.0025
  return inner.totalCharges(tradeValue) + tradeValue * RATE;
  ```
  `SttDecorator` uses `RATE = 0.001`, `GstDecorator` uses `RATE = 0.0005`.
  Each one calls `inner.totalCharges(tradeValue)` *first*, then adds its own
  cut — so calling `totalCharges` on the outermost layer walks all the way
  down to `NoCharge` and sums every layer's cut on the way back up.

This is the **Decorator pattern**: instead of one method with
`brokerage + stt + gst` hardcoded inline, each fee is its own small,
independent object that wraps another `ChargeCalculator` and adds one cut.
`StockBrokerService`'s constructor builds the stack exactly once:
```java
private final ChargeCalculator chargeCalculator =
        new GstDecorator(new SttDecorator(new BrokerageFeeDecorator(new NoCharge())));
```
Read inside-out: `NoCharge` (0) → wrapped by `BrokerageFeeDecorator` (+0.25%)
→ wrapped by `SttDecorator` (+0.1%) → wrapped by `GstDecorator` (+0.05%).
Adding a new fee later (the README mentions stamp duty as an example) means
writing one new `ChargeDecorator` subclass and adding it to this one
constructor line — `executeOrder()` itself never needs to change, since it
only ever calls `chargeCalculator.totalCharges(tradeValue)` on the finished
stack and doesn't know or care how many layers are inside it.

### Step 4 — where does the price actually come from? (`src/external/` and `src/adapter/`)

- **`external/ExternalQuote.java`** and **`external/ExternalMarketFeed.java`**
  — a stand-in for a third-party market-data vendor's client library, with
  its own vocabulary: `fetchQuote(ticker)` returns an `ExternalQuote` (which
  has `getTicker()`/`getLastTradedPrice()`), and it returns `null` — not an
  exception — if that ticker has never had a quote published. This mirrors
  how a real vendor library might be shaped, and deliberately uses different
  words (`ticker`/`lastTradedPrice`) than the broker's own vocabulary
  (`symbol`/`price`) so the adapter's translation is real work, not a
  cosmetic wrapper.
- **`adapter/PriceFeed.java`** — the interface the *broker's own* code
  actually depends on: one method, `getCurrentPrice(symbol)`, returning a
  plain `double`.
- **`adapter/ExternalMarketFeedAdapter.java`** — implements `PriceFeed` by
  calling the external feed's `fetchQuote(symbol)` and translating the
  result: if it's `null`, throw `NoQuoteAvailableException` (turning "no
  quote" from a silent `null` into an explicit failure the rest of the
  broker can catch); otherwise, unwrap `getLastTradedPrice()` and return it
  as a plain `double`.

This is the **Adapter pattern**: `StockBrokerService` and both
`OrderExecutionStrategy` implementations only ever talk to the `PriceFeed`
interface — nothing outside `adapter/` ever touches `ExternalMarketFeed` or
`ExternalQuote` directly. If the broker later swapped to a real market-data
vendor with a totally different API shape, only a new class implementing
`PriceFeed` would need to be written; nothing in `services/` or
`strategy/` would change.

### Step 5 — where things are looked up (`src/repository/`)

- **`AccountRepository.java`** — wraps a `Map<String, Account>`.
  `findByAccountId` throws `AccountNotFoundException` if the id isn't
  registered.
- **`OrderRepository.java`** — wraps a `Map<String, Order>`, plus two useful
  scans: `findPendingBySymbol(symbol)` (every `PENDING` order for one
  symbol — this is what `publishPriceUpdate` rechecks on every tick) and
  `findByAccountId(accountId)` (every order ever placed by one account, used
  by `getOrderHistory`). `findByOrderId` throws `OrderNotFoundException` if
  missing.

### Step 6 — errors (`src/exceptions/`)

Seven distinct failure cases, each its own `RuntimeException` subclass:
- `AccountNotFoundException` — unknown `accountId`.
- `NoQuoteAvailableException` — no price has ever been published for a
  symbol (thrown by the adapter, propagated up).
- `InsufficientFundsException` — a buy order's total cost exceeds the
  account's cash balance.
- `InsufficientHoldingsException` — a sell order's quantity exceeds what
  the account actually holds.
- `OrderNotFoundException` — unknown `orderId`.
- `InvalidOrderException` — a non-positive quantity, or a `LIMIT` order
  placed with no `limitPrice`.
- `IllegalOrderStateException` — trying to cancel an order that isn't
  currently `PENDING` (already `EXECUTED` or already `CANCELLED`).

### Step 7 — the orchestrator (`src/services/StockBrokerService.java`)

Now that you've seen every piece, this file wires them together. The two
methods worth reading closely are `placeOrder` and `publishPriceUpdate` —
full traces are in section 4 below. A few details worth flagging while you
read the source:

- `resolveStrategy(OrderType)` is a small private `switch` — functionally
  the same idea as `SeatPricingStrategyFactory` in the movie-booking problem
  or `SplitStrategyFactory` in splitwise, just kept as a private method here
  rather than promoted to its own `factory/` package, since there's only one
  place that needs it.
- `tryGetCurrentPrice(symbol)` wraps `priceFeed.getCurrentPrice(symbol)` in
  a try/catch that turns `NoQuoteAvailableException` into a plain `null`
  return. This lets `placeOrder` treat "no quote yet" as a normal case to
  branch on (`currentPrice != null && ...`) instead of a thrown exception,
  while `publishPriceUpdate` (which only ever calls `getCurrentPrice` for a
  symbol it just received a quote for) never needs that safety net.
- In `placeOrder`, the `Order` object is constructed — consuming an
  `orderSequence` id — *before* the code even attempts to check if it's
  executable. This is deliberate but has a real, visible consequence: see
  the "O4/O5 never existed" discussion in section 5.

### Step 8 — the runner (`src/Main.java`)

Not part of the "real" system — a test harness. It reads a text script line
by line (`test/input/scenario.txt`), turns each line into a call on
`StockBrokerService`, and writes what happened to `test/output/output.txt`.

---

## 4. Picture of two full flows

### Flow A — a market buy order that executes immediately

```
Main.java (reads "ORDER alice BUY AAPL MARKET 10 NONE", after "PRICE AAPL 150.00")
   |
   v
StockBrokerService.placeOrder("alice", "AAPL", BUY, MARKET, 10, null)
   |
   | accountRepository.findByAccountId("alice")   -> confirms alice exists
   | quantity (10) > 0                            -> OK
   | type == LIMIT && limitPrice == null?          -> no, it's MARKET, skip
   |
   | new Order("O1", "alice", "AAPL", BUY, MARKET, 10, null)   -> status starts PENDING
   |
   | resolveStrategy(MARKET)                       -> new MarketOrderStrategy()
   | tryGetCurrentPrice("AAPL")
   |     priceFeed.getCurrentPrice("AAPL")   (the PriceFeed interface, i.e. the adapter)
   |         externalMarketFeed.fetchQuote("AAPL")  -> ExternalQuote(ticker="AAPL", lastTradedPrice=150.0)
   |         quote != null -> return quote.getLastTradedPrice() = 150.0
   |     -> currentPrice = 150.0
   |
   | strategy.isExecutable(order, 150.0)            -> MarketOrderStrategy always returns true
   | currentPrice != null && executable            -> executeOrder(order, 150.0)
   |     account = accountRepository.findByAccountId("alice")
   |     tradeValue = 150.0 * 10 = 1500.0
   |     charges = chargeCalculator.totalCharges(1500.0)
   |         GstDecorator:       inner.totalCharges(1500) + 1500*0.0005
   |           SttDecorator:       inner.totalCharges(1500) + 1500*0.001
   |             BrokerageFeeDecorator: inner.totalCharges(1500) + 1500*0.0025
   |               NoCharge:               0.0
   |             = 0.0 + 3.75 = 3.75
   |           = 3.75 + 1.5 = 5.25
   |         = 5.25 + 0.75 = 6.0
   |     side == BUY -> totalCost = 1500.0 + 6.0 = 1506.0
   |     1506.0 > account.getCashBalance() (100000)?  no -> proceed
   |     account.debit(1506.0)          -> cash: 100000 -> 98494.0
   |     account.addHolding("AAPL", 10) -> holdings: {AAPL: 10}
   |     order.markExecuted(150.0, 6.0) -> status becomes EXECUTED
   |
   | orderRepository.save(order)
   v
returns the Order to Main.java, which prints:
"OK order O1 BUY 10 AAPL (MARKET) -> status=EXECUTED @150.0 charges=6.0"
```

### Flow B — a price tick triggers a pending limit order

```
Main.java (reads "PRICE AAPL 138.00", after bob placed "ORDER bob BUY AAPL LIMIT 20 140" earlier and it stayed PENDING)
   |
   v
StockBrokerService.publishPriceUpdate("AAPL", 138.0)
   |
   | externalMarketFeed.publishQuote("AAPL", 138.0)   -> the "vendor" now has a fresh quote
   | currentPrice = priceFeed.getCurrentPrice("AAPL") -> 138.0 (via the adapter, same as Flow A)
   |
   | orderRepository.findPendingBySymbol("AAPL")      -> [bob's O3 buy-limit@140 order]
   |     (alice's O2 sell-limit@160 order is also PENDING at this point, but for AAPL too --
   |      it gets checked in this same loop and simply doesn't qualify yet)
   |
   | for O3:
   |     resolveStrategy(LIMIT)                        -> new LimitOrderStrategy()
   |     strategy.isExecutable(O3, 138.0)
   |         O3.getSide() == BUY -> return 138.0 <= O3.getLimitPrice() (140.0)  -> true
   |     -> executable, so:
   |     executeOrder(O3, 138.0)
   |         tradeValue = 138.0 * 20 = 2760.0
   |         charges = chargeCalculator.totalCharges(2760.0) = 2760.0 * 0.004 = 11.04
   |         totalCost = 2760.0 + 11.04 = 2771.04
   |         2771.04 > bob's cash (50000)?  no -> proceed
   |         bob.debit(2771.04)             -> cash: 50000 -> 47228.96
   |         bob.addHolding("AAPL", 20)
   |         O3.markExecuted(138.0, 11.04)  -> status becomes EXECUTED
   |         (no exception thrown, so the try/catch around executeOrder in the
   |          loop has nothing to swallow)
   v
Main.java prints: "OK price AAPL -> 138.0"
(the status change is silent from the caller's point of view -- it only
shows up later, when PORTFOLIO bob or HISTORY bob is queried)
```

Notice `publishPriceUpdate` wraps each `executeOrder` call in a try/catch
for `InsufficientFundsException`/`InsufficientHoldingsException` and simply
does nothing if one is thrown — the order is left `PENDING` rather than
failed, and the loop moves on to check the *next* pending order. A price
tick that triggers five pending orders where the third one can't actually
be afforded still lets the fourth and fifth execute.

---

## 5. Reading the actual captured run (`test/output/output.txt`)

```
> ORDER alice BUY AAPL MARKET 10 NONE
OK order O1 BUY 10 AAPL (MARKET) -> status=EXECUTED @150.0 charges=6.0
> PORTFOLIO alice
PORTFOLIO alice cash=98494.0 holdings={AAPL=10}
```

This is Flow A above, numbers matching exactly: trade value $1500, charges
$6.00 (0.25% + 0.1% + 0.05% = 0.4% of $1500), total cost $1506, cash
100000 - 1506 = **98494.0**.

```
> ORDER alice SELL AAPL LIMIT 5 160
OK order O2 SELL 5 AAPL (LIMIT) -> status=PENDING
> ORDER bob BUY AAPL LIMIT 20 140
OK order O3 BUY 20 AAPL (LIMIT) -> status=PENDING
```

Both limit orders are placed while the price is still $150: alice's
sell-limit needs the price to rise to $160+ (it hasn't), bob's buy-limit
needs it to drop to $140 or below (it hasn't). `LimitOrderStrategy` returns
`false` for both at placement time, so `placeOrder` falls through to saving
them as `PENDING` rather than executing.

```
> PRICE AAPL 145.00
OK price AAPL -> 145.0
> PRICE AAPL 138.00
OK price AAPL -> 138.0
> PORTFOLIO bob
PORTFOLIO bob cash=47228.96 holdings={AAPL=20}
```

The $145 tick doesn't trigger either pending order (145 isn't `<= 140` for
bob, isn't `>= 160` for alice). The $138 tick is Flow B above: bob's buy
executes at $138 for 20 shares. Trade value 138 * 20 = $2760, charges 2760
* 0.004 = $11.04, total cost $2771.04, cash 50000 - 2771.04 =
**47228.96** — matching the captured output exactly.

```
> PRICE AAPL 165.00
OK price AAPL -> 165.0
> PORTFOLIO alice
PORTFOLIO alice cash=99315.7 holdings={AAPL=5}
> HISTORY alice
HISTORY alice
  O1 BUY 10 AAPL (MARKET) -> EXECUTED @150.0 charges=6.0
  O2 SELL 5 AAPL (LIMIT) -> EXECUTED @165.0 charges=3.3000000000000003
```

Now alice's sell-limit triggers (165 >= 160). Trade value 165 * 5 = $825,
charges 825 * 0.004 = $3.30 (note the floating-point noise —
`3.3000000000000003` instead of a clean `3.3` — a real consequence of
`double` arithmetic that the README's "Known gaps" flags as a reason a real
system would use `BigDecimal`). Sell proceeds = 825 - 3.3 = $821.70, credited
to alice's cash: 98494.0 + 821.7 = **99315.7**, matching exactly. Her
holdings drop from 10 to 5 shares (10 bought, 5 sold).

```
> ORDER alice SELL AAPL MARKET 100 NONE
ERROR InsufficientHoldingsException: Insufficient holdings of AAPL: requested 100, held 5
> ORDER alice BUY GOOG MARKET 5 NONE
ERROR NoQuoteAvailableException: No market price available for GOOG
> ORDER alice BUY AAPL LIMIT 5 NONE
ERROR InvalidOrderException: Limit orders require a limit price
> ORDER alice BUY AAPL MARKET -5 NONE
ERROR InvalidOrderException: Quantity must be positive
```

Four different validation failures, in the order `placeOrder` actually
checks them: holdings shortfall (caught inside `executeOrder`, since a
market sell is always "executable" and only fails once the funds/holdings
check runs), a symbol with no published price at all (`GOOG` was never
given a `PRICE` line), a limit order missing its price, and a non-positive
quantity.

```
> ORDER bob SELL AAPL LIMIT 5 200
OK order O6 SELL 5 AAPL (LIMIT) -> status=PENDING
```

The order id jumps from the O1-O3 already used straight to **O6** — not
O4. This is the direct, visible consequence of the design note in section
3, Step 7: the two rejected orders just above (`ORDER alice SELL AAPL
MARKET 100 ...` and `ORDER alice BUY GOOG MARKET 5 ...`) each still called
`new Order("O" + orderSequence.getAndIncrement(), ...)` before their
validation/execution failed, so they silently consumed ids O4 and O5 —
without ever being saved to `OrderRepository`. The scenario file's own
comment above this line spells this out, and the next two lines prove it:

```
> CANCEL O6
OK cancelled order O6
> CANCEL O6
ERROR IllegalOrderStateException: Cannot cancel order in status CANCELLED
...
> CANCEL O4
ERROR OrderNotFoundException: No order with id O4
```

Cancelling O6 twice gives two *different* exceptions on purpose: the first
succeeds and flips it to `CANCELLED`; the second fails with
`IllegalOrderStateException` because `cancelOrder`'s guard requires
`status == PENDING`. But `CANCEL O4` fails with `OrderNotFoundException`,
not `IllegalOrderStateException` — proving O4 isn't sitting somewhere in a
non-cancellable state, it genuinely was **never saved** to
`OrderRepository` at all.

```
> ORDER bob BUY AAPL MARKET 100000 NONE
ERROR InsufficientFundsException: Insufficient funds: need 1.6566E7, available 47228.96
```

100000 shares * $165 (the last published AAPL price) = $16,500,000 trade
value; charges = 16,500,000 * 0.004 = $66,000; total cost = $16,566,000,
printed by Java's default `double` formatting as `1.6566E7` — matching
`need 1.6566E7` exactly. Bob's cash is still `47228.96` (his only trade was
the O3 buy; the cancelled O6 sell-limit never executed and therefore never
touched his cash), so this order is correctly rejected.

---

## 6. Things to try / test yourself

You can edit `test/input/scenario.txt` and re-run with:
```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

1. **Watch a limit order stay pending across several ticks.**
   Place `ORDER alice BUY AAPL LIMIT 10 100` (well below any price in the
   scenario) and then a handful of `PRICE AAPL ...` lines that never dip to
   $100 or below. Confirm via `HISTORY alice` that it's still `PENDING`
   after every tick — `LimitOrderStrategy.isExecutable` should keep
   returning `false`.

2. **Verify the charge stack really is additive.**
   Pick any executed trade's `tradeValue` (price * quantity) from a
   `PORTFOLIO`/`HISTORY` line and multiply it by `0.0025 + 0.001 + 0.0005 =
   0.004`. It should match the order's `charges` field exactly (mind
   floating-point noise like the `3.3000000000000003` seen in section 5).

3. **Trigger multiple pending orders on one tick.**
   Place two buy-limit orders on the same symbol with different limit
   prices that are both above a price you're about to publish (e.g. limits
   of 150 and 145, then `PRICE X 140`). One `PRICE` command should execute
   both in the same `publishPriceUpdate` call — confirm via `HISTORY` that
   both flipped to `EXECUTED` from that single tick.

4. **Force a pending order to be skipped, not failed, due to funds.**
   Place a buy-limit order your account can't actually afford at its limit
   price (e.g. limit price high enough that quantity * price exceeds cash),
   let the price tick to trigger it, and confirm via `HISTORY` that it's
   still `PENDING` afterward (not `CANCELLED`, not erroring the `PRICE`
   command) — this is the try/catch-and-skip behavior in
   `publishPriceUpdate` from Flow B's explanation.

5. **Confirm order ids really do get "burned" by rejections.**
   Deliberately place an order you know will fail (e.g. an unaffordable
   market buy) immediately followed by one that should succeed. Check the
   successful order's id — it should skip over the number the failed one
   consumed, the same way O4/O5 were skipped in the real run.

6. **Break something on purpose.**
   Try `ORDER alice BUY AAPL LIMIT 5 abc` (a non-number for the limit
   price) or an unrecognized `OrderSide`/`OrderType` string like `ORDER
   alice HOLD AAPL MARKET 5 NONE` — trace the resulting error back to
   `Double.parseDouble`/`OrderSide.valueOf` in `Main.execute`.
