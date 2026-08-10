# Ecommerce / Inventory Management

> **Mode: Learning** — this design (happy flow, class breakdown, pattern
> choices) was authored by Claude, not the user. See `CLAUDE.md` → "Mode:
> Learning" for what that implies about ownership of the decisions below.

An Amazon/Zepto-style catalog + cart + checkout system: an admin stocks
products, customers build a cart and check out, and each resulting order
moves through a delivery lifecycle (or gets cancelled, which restocks it).

## Happy flow

1. An admin adds products to the catalog via `EcommerceService.addProduct()`.
2. A customer adds items to their cart (`addToCart()`); each customer has
   exactly one running cart, created on first use.
3. On `checkout()`, the cart is run through a validation pipeline — stock
   availability, then server-side price/coupon recomputation, then payment
   authorization — before anything is mutated. Any failure aborts the
   checkout with nothing reserved.
4. Once the pipeline passes, stock is decremented per item, an `Order` is
   assembled (`OrderBuilder`) with a price snapshot per line item, saved,
   and the cart is cleared.
5. The order starts `PLACED` and can be driven through `confirmOrder()` →
   `shipOrder()` → `deliverOrder()`, each a legality-checked transition.
6. `cancelOrder()` is only legal from `PLACED`/`CONFIRMED`; on success it
   restocks every item back onto the catalog.

## Design patterns used

- **Chain of Responsibility** — `chain/OrderValidationHandler.java` with
  `StockAvailabilityHandler` → `PriceValidationHandler` → 
  `PaymentAuthorizationHandler`, wired once in `EcommerceService`'s
  constructor. Each link either passes silently or throws (`OutOfStockException`,
  `PaymentDeclinedException`); `checkout()` doesn't know how many checks
  exist or what order-specific data each one needs — adding a new check
  (e.g. fraud screening) means adding a handler class, not editing
  `checkout()`. `PriceValidationHandler` also recomputes the total from the
  catalog's current prices (never trusts a client-supplied amount) and
  applies the coupon discount there, so the amount `PaymentAuthorizationHandler`
  authorizes is always server-derived.
- **State** — `state/OrderState.java` (interface with throwing defaults) plus
  `PlacedState`/`ConfirmedState`/`ShippedState`/`DeliveredState`/`CancelledState`
  singletons. `Order` holds the current `OrderState` and its
  `confirm()/ship()/deliver()/cancel()` methods just delegate to it. Each
  concrete state overrides only the transitions it allows (e.g.
  `ShippedState` has no `cancel()` override, so cancelling a shipped order
  falls through to the interface default and throws
  `InvalidOrderStateException`) — adding a status is a new small class, not
  a growing switch statement shared by four transition methods.
- **Builder** — `builder/OrderBuilder.java`. `checkout()` adds one
  `OrderItem` per cart line as it iterates and reserves stock, so the item
  list is only fully known partway through the method; `build()` then
  validates required fields (`orderId`, `customerId`, `shippingAddress`,
  at least one item) before constructing the `Order`, which keeps that
  incremental-assembly logic out of `Order`'s constructor.

## Structure

```
ecommerce/
  src/
    model/       Product, CartItem, Cart, OrderItem, Order, OrderStatus
    state/       OrderState + Placed/Confirmed/Shipped/Delivered/CancelledState
    chain/       OrderValidationHandler family + OrderValidationContext
    builder/     OrderBuilder
    repository/  ProductRepository, OrderRepository (in-memory)
    exceptions/  ProductNotFoundException, OutOfStockException, InvalidOrderStateException,
                 PaymentDeclinedException, OrderNotFoundException, EmptyCartException
    services/    EcommerceService (the only public entry point)
    Main.java    Reads a command script, drives the service, writes a transcript
  test/
    input/scenario.txt   Happy path (checkout → deliver) + every edge case below
    output/output.txt    Captured run transcript
  diagrams/
    generate.py       Data-only script that builds ecommerce.drawio via docs/tooling/drawio_uml.py
    ecommerce.drawio   Class diagram + 3 sequence diagrams (checkout, lifecycle, cancel)
```

## Running it

```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

Requires the VS Code "Extension Pack for Java" (Microsoft) for the
`.vscode/launch.json` run config to work — open the `ecommerce/` folder
itself as the workspace root, then use the "Run Main (scenario.txt)" config.

## Known gaps (flagged, not fixed)

- No persistence — all state is in-memory and lost on process exit.
- No concurrency control — `checkout()`/`cancelOrder()` aren't synchronized,
  so two threads acting on the same product's stock could race between the
  Chain of Responsibility's stock check and the actual `decreaseStock()`
  call (classic check-then-act).
- Currency is a raw `double`; a real system would want a fixed-point/
  `BigDecimal` money type to avoid floating-point drift.
- `PaymentAuthorizationHandler` is a mock — it declines a single hardcoded
  test token (`INVALID_CARD`) rather than integrating a real gateway.
- Coupons are a hardcoded `Map<String,Double>` inside `PriceValidationHandler`
  rather than their own model/repository — fine for this exercise, but a
  real system would want coupons to be data, not code.
- Cancelling a `SHIPPED` order isn't supported (`ShippedState` has no
  `cancel()`), which is realistic once a package is in transit, but there's
  no "return/refund after delivery" flow either — cancellation is
  pre-shipment only.
