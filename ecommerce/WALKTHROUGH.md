# Ecommerce / Inventory Management — Simple Walkthrough

This file explains the whole codebase in plain language, in the order you
should read it. No prior context assumed.

---

## 1. The story in one paragraph

You're building the backend of an online store — think Amazon or Zepto at a
small scale. An admin stocks products with a price and a quantity. A
customer adds items to their cart, and when they check out, the system has
to run a whole series of checks — is there enough stock, what's the real
total after any coupon discount, does the payment actually go through —
*before* it commits to anything. Only if every check passes does it
actually take stock away from inventory and create a real `Order`. From
there the order has a delivery lifecycle: it starts `PLACED`, and can move
through `CONFIRMED` -> `SHIPPED` -> `DELIVERED`, or get cancelled early
(which gives the stock back).

---

## 2. The one door you're allowed to knock on

`src/services/EcommerceService.java` is the **only** class anything outside
the package is meant to call. Everything else (`model`, `chain`, `builder`,
`state`, `repository`, `exceptions`) is a helper it uses internally.

| Method | What it does |
|---|---|
| `addProduct(sku, name, price, initialStock)` | Admin operation: add a product to the catalog |
| `addToCart(customerId, sku, quantity)` | Add (or increase) an item in a customer's cart |
| `removeFromCart(customerId, sku)` | Remove an item from a customer's cart |
| `viewCart(customerId)` | See a customer's current cart |
| `checkout(customerId, shippingAddress, couponCode, paymentMethod)` | Validate the cart, reserve stock, create an `Order` |
| `confirmOrder(orderId)` | `PLACED` -> `CONFIRMED` |
| `shipOrder(orderId)` | `CONFIRMED` -> `SHIPPED` |
| `deliverOrder(orderId)` | `SHIPPED` -> `DELIVERED` |
| `cancelOrder(orderId)` | Cancel (only legal from `PLACED`/`CONFIRMED`), restocking every item |
| `getOrderStatus(orderId)` | The order's current `OrderStatus` |
| `getInventoryReport()` | A text summary of every product's current stock |

---

## 3. Read the code in this order

### Step 1 — the building blocks (`src/model/`)

These are mostly plain data holders, with a couple of small but important
behaviors.

- **`Product.java`** — sku, name, price, and a mutable `stockQuantity`,
  with `decreaseStock(quantity)`/`increaseStock(quantity)` to adjust it.
  This is the *one* object in the whole system that represents "how much
  of this thing do we actually have" — everything else (carts, orders)
  just refers to a `Product`, never duplicates its stock count.
- **`CartItem.java`** — a `Product` plus a `quantity` — one line of a
  cart. Plain data, no behavior.
- **`Cart.java`** — one customer's shopping cart: a map from SKU to
  `CartItem`. `addItem(product, quantity)` uses `Map.merge()` so adding
  the same SKU twice combines quantities into one line instead of two
  separate entries — worth reading that one line closely:
  ```java
  itemsBySku.merge(product.getSku(), new CartItem(product, quantity),
          (existing, added) -> new CartItem(product, existing.getQuantity() + added.getQuantity()));
  ```
  `removeItem`, `getItems` (a defensive copy), `isEmpty`, and `clear`
  round out the class.
- **`OrderItem.java`** — like `CartItem`, but for a finalized order: a
  `Product`, `quantity`, and — critically — its own
  `priceAtPurchase`, a **frozen snapshot** of what the customer actually
  paid per unit at checkout time, independent of whatever the product's
  live price is later. `getSubtotal()` multiplies the two.
- **`OrderStatus.java`** — enum: `PLACED`, `CONFIRMED`, `SHIPPED`,
  `DELIVERED`, `CANCELLED`. Just a label; the actual "what transitions are
  legal from here" logic lives in `state/` (Step 3).
- **`Order.java`** — the finalized purchase: id, customer, an immutable
  `List<OrderItem>` (via `List.copyOf`), shipping address, total amount,
  coupon code, and a current `OrderState`. Its class comment spells out
  the design choice directly: `confirm()/ship()/deliver()/cancel()` each
  just delegate one line to the current state
  (`state = state.confirm();`) rather than this class holding an
  `OrderStatus` enum and switching on it — "each status's legal next-moves
  live with that status instead of in a big conditional this class would
  otherwise need to grow for every new status."

### Step 2 — what happens when someone checks out? (`src/chain/`)

This is the **Chain of Responsibility** pattern — the same pattern used
for the ATM problem's cash dispensing, but here applied to a pipeline of
independent *validation* steps instead of denomination math.

- **`OrderValidationHandler.java`** — the abstract base every check
  extends. It holds a `next` pointer and exposes one **final** (can't be
  overridden) method, `handle(context)`, which calls the subclass's own
  `validate(context)` first and then, if there is one, hands off to
  `next.handle(context)`. Subclasses only ever implement `validate()` —
  they never touch the chaining logic itself. Any check can reject the
  whole checkout simply by throwing out of `validate()`, which stops the
  chain right there (the exception propagates up, `next.handle()` never
  gets called).
- **`OrderValidationContext.java`** — the one object threaded through
  every handler in the chain: the `Cart`, the `ProductRepository` (so
  handlers can look up live product data), the `couponCode`, the
  `paymentMethod`, and a `computedTotal` field that starts at `0.0` and
  gets **written** by one handler (`PriceValidationHandler`) for a later
  handler (and the caller, after the whole chain finishes) to read. This
  is how information flows *forward* through the chain without every
  handler needing a reference to every other handler.
- **`StockAvailabilityHandler.java`** — the first link: for every cart
  line, look up the product's *current* stock and throw
  `OutOfStockException` if the requested quantity exceeds what's
  available. Read-only — it never touches `stockQuantity` itself.
- **`PriceValidationHandler.java`** — the second link: recomputes the
  order's subtotal from the catalog's *current* prices (never trusts a
  client-supplied amount — the class comment is explicit about this),
  applies a coupon discount if `couponCode` matches one of the two
  hardcoded entries in `COUPON_DISCOUNTS` (`SAVE10` -> 10% off, `SAVE20`
  -> 20% off; anything else, including `null`, gets `0.0`), and writes the
  result into `context.setComputedTotal(...)`.
- **`PaymentAuthorizationHandler.java`** — the last link: a mock payment
  gateway. It declines exactly one hardcoded token, `"INVALID_CARD"`,
  throwing `PaymentDeclinedException`; every other string is treated as a
  successful charge. The class comment is upfront that this exists "so the
  design has a way to exercise a failed checkout without wiring an actual
  payment gateway."
- All three are wired together **once**, in `EcommerceService`'s
  constructor:
  ```java
  OrderValidationHandler stockCheck = new StockAvailabilityHandler();
  stockCheck.linkWith(new PriceValidationHandler())
          .linkWith(new PaymentAuthorizationHandler());
  ```
  `checkout()` then just calls `validationChain.handle(context)` once —
  it doesn't know or care how many checks exist or what each one needs.
  Adding a new check later (the README suggests fraud screening as an
  example) means writing one new `OrderValidationHandler` subclass and
  adding one more `.linkWith(...)` call, not editing `checkout()`'s logic
  at all.

### Step 3 — what can an order do next? (`src/state/`)

This is the **State** pattern, structured almost identically to the ATM
and Chess problems in this same repo: an interface with `default`
implementations that throw, so each concrete state only needs to override
the handful of transitions it actually allows.

- **`OrderState.java`** — the interface: `getStatus()` plus four
  transition methods (`confirm()`, `ship()`, `deliver()`, `cancel()`),
  each with a `default` body that throws `InvalidOrderStateException`
  naming the current status. Any transition a concrete state doesn't
  override automatically falls through to this throwing default.
- **`PlacedState.java`** — overrides `confirm()` (-> `ConfirmedState`) and
  `cancel()` (-> `CancelledState`). A freshly placed order can be
  confirmed or cancelled, nothing else.
- **`ConfirmedState.java`** — overrides `ship()` (-> `ShippedState`) and
  `cancel()` (-> `CancelledState`). Still cancellable at this stage.
- **`ShippedState.java`** — overrides **only** `deliver()` (->
  `DeliveredState`). No `cancel()` override — its class comment spells out
  why: "once an order has shipped it's already in transit, so
  cancellation is no longer offered." Trying to cancel a shipped order
  falls through to the interface default and throws.
- **`DeliveredState.java`** — overrides nothing except `getStatus()`. A
  terminal state: every transition throws.
- **`CancelledState.java`** — same shape as `DeliveredState`: terminal,
  overrides nothing but `getStatus()`.

Every state is a `public static final INSTANCE` singleton with a private
constructor — there's no reason to allocate a new `ConfirmedState` object
every time some order gets confirmed, since the object carries no
order-specific data (that all lives on `Order` itself).

### Step 4 — assembling an order piece by piece (`src/builder/OrderBuilder.java`)

This is the **Builder** pattern. `OrderBuilder` has one setter-like method
per `Order` field (`orderId(...)`, `customerId(...)`, `shippingAddress(...)`,
`totalAmount(...)`, `couponCode(...)`), each returning `this` so calls
chain fluently, plus a repeatable `addItem(product, quantity,
priceAtPurchase)` that appends to a growing internal list. `build()` is
the final step: it checks the required fields aren't null and that at
least one item was added (throwing `IllegalStateException` if not), then
constructs the actual immutable `Order`.

The class comment explains exactly why this exists instead of just calling
`new Order(...)` directly: `checkout()` (Step 6) adds one `OrderItem` *as
it iterates the cart and reserves stock line by line* — so the full item
list genuinely isn't known until partway through the method. Building that
incrementally, with a single validity check right before construction,
would be awkward to express as one big constructor call up front; the
builder lets `checkout()` assemble the order gradually and only "finish"
it once every line has been processed successfully.

### Step 5 — looking things up (`src/repository/`)

Two small repositories, both the same shape as elsewhere in this repo: a
`Map` plus save/find, throwing a specific not-found exception on a miss.

- **`ProductRepository.java`** — keyed by SKU;
  `findBySku` throws `ProductNotFoundException`; `getAllProducts()`
  returns the live `Collection<Product>` values for the inventory report.
- **`OrderRepository.java`** — keyed by order id;
  `findById` throws `OrderNotFoundException`.

### Step 6 — errors (`src/exceptions/`)

Six small `RuntimeException` subclasses, each wrapping just a message:

- `ProductNotFoundException` — unknown SKU.
- `OutOfStockException` — requested quantity exceeds available stock
  (thrown by `StockAvailabilityHandler`).
- `InvalidOrderStateException` — an illegal state transition was
  attempted (thrown by an `OrderState` default).
- `PaymentDeclinedException` — the mock gateway declined the payment
  method (thrown by `PaymentAuthorizationHandler`).
- `OrderNotFoundException` — unknown order id.
- `EmptyCartException` — `checkout()` called with nothing in the cart.

### Step 7 — the orchestrator (`src/services/EcommerceService.java`)

Now that you've seen every collaborator, `checkout()` is the method worth
reading most carefully — it's the one place everything above gets tied
together, and its class-level comment states the intent directly: *"Runs
the cart through the validation chain (stock -> price/coupon -> payment
authorization) before touching any state, so a rejection at any step
leaves inventory and the cart untouched."* Walking through it in order:

1. `getCart(customerId)` — carts are created lazily on first use, via
   `cartsByCustomerId.computeIfAbsent(customerId, Cart::new)` in the
   private `getCart()` helper. Every customer has exactly one running
   cart.
2. `cart.isEmpty()` -> throw `EmptyCartException` immediately if so.
3. Build an `OrderValidationContext` and run it through
   `validationChain.handle(context)` — this is where
   `StockAvailabilityHandler` -> `PriceValidationHandler` ->
   `PaymentAuthorizationHandler` all run in sequence. **Nothing below this
   line executes if any of the three throws.**
4. Only now: start an `OrderBuilder`, and — for each cart line — actually
   call `product.decreaseStock(item.getQuantity())` (the real inventory
   mutation) and `builder.addItem(product, item.getQuantity(),
   product.getPrice())` (freezing that line's price into the order).
5. `builder.build()`, save the resulting `Order` into `orderRepository`,
   `cart.clear()`, return the order.

The rest of the class is thin, single-purpose delegation:
`confirmOrder`/`shipOrder`/`deliverOrder` each just call the matching
method on the looked-up `Order` (which itself delegates to its current
`OrderState`), and `cancelOrder` calls `order.cancel()` **then**, only if
that succeeded (didn't throw), loops over every `OrderItem` and calls
`increaseStock(...)` back on the matching `Product` — restocking is a
direct consequence of a successful state transition, not a separate step
a caller could forget.

### Step 8 — the runner (`src/Main.java`)

Not part of the "real" system — a test harness. It reads a text file line
by line (`test/input/scenario.txt`), turns each line into a call on
`EcommerceService`, and writes what happened to `test/output/output.txt`.
Its command language: `PRODUCT`, `ADDCART`, `REMOVECART`, `CART`,
`CHECKOUT`, `CONFIRM`, `SHIP`, `DELIVER`, `CANCEL`, `STATUS`, `INVENTORY`.
All six of `EcommerceService`'s custom exception types are caught in one
`catch` clause and turned into an `ERROR <ExceptionClassName>: <message>`
line.

---

## 4. Order of operations — two traces through the real code

### Trace A — a successful checkout with a coupon

```
Main.java script:
  ADDCART alice LAPTOP001 1
  ADDCART alice MOUSE001 2
  CHECKOUT alice 123_Main_St SAVE10 CARD

EcommerceService.checkout("alice", "123_Main_St", "SAVE10", "CARD")
   | cart = getCart("alice")   -- has 1x LAPTOP001, 2x MOUSE001
   | cart.isEmpty()? no
   | context = new OrderValidationContext(cart, productRepository, "SAVE10", "CARD")
   | validationChain.handle(context)
   |    StockAvailabilityHandler.validate(context)
   |        LAPTOP001: requested 1 <= available 5 -- OK
   |        MOUSE001:  requested 2 <= available 50 -- OK
   |        (calls next.handle(context))
   |    PriceValidationHandler.validate(context)
   |        subtotal = 1200.0*1 + 25.0*2 = 1250.0
   |        discount = COUPON_DISCOUNTS.get("SAVE10") = 0.10
   |        context.setComputedTotal(1250.0 * (1 - 0.10)) -> 1125.0
   |        (calls next.handle(context))
   |    PaymentAuthorizationHandler.validate(context)
   |        "CARD" != "INVALID_CARD" -- no throw
   |        (next == null -- chain ends here)
   | builder = new OrderBuilder().orderId("O1").customerId("alice")
   |               .shippingAddress("123_Main_St").totalAmount(1125.0).couponCode("SAVE10")
   | for LAPTOP001: product.decreaseStock(1)  -- 5 -> 4
   |                builder.addItem(laptop, 1, 1200.0)
   | for MOUSE001:  product.decreaseStock(2)  -- 50 -> 48
   |                builder.addItem(mouse, 2, 25.0)
   | order = builder.build()   -- Order O1, PlacedState.INSTANCE
   | orderRepository.save(order)
   | cart.clear()
   v
returns order
Main.java prints "OK checkout -> order O1 total=$1125.0 status=PLACED"
```

Notice stock was decremented (step 4) only *after* the entire chain
(step 3) passed with no exceptions — exactly the "validate everything,
then commit everything" shape the class comment promises.

### Trace B — a checkout rejected mid-chain leaves stock untouched

```
Main.java script:
  ADDCART bob KEYBOARD001 3
  CHECKOUT bob 456_Oak_Ave NONE INVALID_CARD

EcommerceService.checkout("bob", "456_Oak_Ave", null, "INVALID_CARD")
   | cart has 3x KEYBOARD001 (stock is 20 at this point)
   | context = new OrderValidationContext(cart, productRepository, null, "INVALID_CARD")
   | validationChain.handle(context)
   |    StockAvailabilityHandler.validate -- 3 <= 20, OK, calls next
   |    PriceValidationHandler.validate -- computes 75.0*3=225.0, no coupon, calls next
   |    PaymentAuthorizationHandler.validate
   |        "INVALID_CARD".equals("INVALID_CARD") -- true
   |        throws PaymentDeclinedException("Payment authorization declined for method INVALID_CARD")
   v
exception propagates all the way up through handle() -> handle() -> checkout()
   -- builder is never even constructed, decreaseStock() is never called,
      cart.clear() never runs -- bob's cart AND KEYBOARD001's stock are
      exactly as they were before this call
```

---

## 5. Reading the actual captured run (`test/output/output.txt`)

The coupon math, verified against real numbers:

```
> CART alice
CART alice
  1x LAPTOP001 (Laptop_Pro) @ $1200.0
  2x MOUSE001 (Wireless_Mouse) @ $25.0
> CHECKOUT alice 123_Main_St SAVE10 CARD
OK checkout -> order O1 total=$1125.0 status=PLACED
```

1x$1200.0 + 2x$25.0 = $1250.0 subtotal; `SAVE10` is a 10% discount, so
$1250.0 * 0.90 = $1125.0 — matching the printed total exactly, and proving
`PriceValidationHandler` really did recompute from catalog prices rather
than trusting anything the caller supplied.

Stock actually decrementing after a successful checkout:

```
> INVENTORY
INVENTORY
LAPTOP001 Laptop_Pro $1200.0 stock=5
...
> CHECKOUT alice 123_Main_St SAVE10 CARD
OK checkout -> order O1 total=$1125.0 status=PLACED
> INVENTORY
INVENTORY
LAPTOP001 Laptop_Pro $1200.0 stock=4
MOUSE001 Wireless_Mouse $25.0 stock=48
KEYBOARD001 Mechanical_KB $75.0 stock=20
```

LAPTOP001 goes from `stock=5` to `stock=4` (1 sold), MOUSE001 from `50` to
`48` (2 sold) — a direct, real proof that `product.decreaseStock(...)` ran
for exactly the quantities in the order.

The full lifecycle, then a rejected cancellation:

```
> CONFIRM O1
OK order O1 confirmed
> SHIP O1
OK order O1 shipped
> DELIVER O1
OK order O1 delivered
> STATUS O1
STATUS O1 -> DELIVERED
> CANCEL O1
ERROR InvalidOrderStateException: Cannot cancel order in status DELIVERED
```

`DeliveredState` overrides nothing but `getStatus()`, so `cancel()` falls
through to the interface default and throws — exactly as designed for a
terminal state.

Stock check rejecting a checkout before anything else even runs:

```
> ADDCART bob KEYBOARD001 100
OK added 100x KEYBOARD001 to bob's cart
> CHECKOUT bob 456_Oak_Ave NONE CARD
ERROR OutOfStockException: Insufficient stock for KEYBOARD001: requested 100, available 20
```

`StockAvailabilityHandler` is the *first* link in the chain, so this fails
before `PriceValidationHandler` or `PaymentAuthorizationHandler` ever run
at all.

The payment-decline case, with the inventory proof that nothing leaked
through:

```
> CHECKOUT bob 456_Oak_Ave NONE INVALID_CARD
ERROR PaymentDeclinedException: Payment authorization declined for method INVALID_CARD
> INVENTORY
INVENTORY
LAPTOP001 Laptop_Pro $1200.0 stock=4
MOUSE001 Wireless_Mouse $25.0 stock=48
KEYBOARD001 Mechanical_KB $75.0 stock=20
```

KEYBOARD001 is still `stock=20` — completely untouched — even though this
checkout got all the way through `StockAvailabilityHandler` and
`PriceValidationHandler` successfully before failing on the last link.
This is Trace B, proven with real numbers.

Cancelling restocks correctly:

```
> CHECKOUT bob 456_Oak_Ave NONE CARD
OK checkout -> order O2 total=$225.0 status=PLACED
> INVENTORY
INVENTORY
...
KEYBOARD001 Mechanical_KB $75.0 stock=17
> CANCEL O2
OK order O2 cancelled, stock restocked
> INVENTORY
INVENTORY
...
KEYBOARD001 Mechanical_KB $75.0 stock=20
```

KEYBOARD001 drops from `20` to `17` (3 sold in order O2), then goes right
back to `20` after `CANCEL O2` — proof `cancelOrder()`'s
`increaseStock(item.getQuantity())` loop restores exactly what was
reserved.

The remaining edge cases, each a distinct exception type:

```
> ADDCART carol GHOST999 1
ERROR ProductNotFoundException: No product with SKU GHOST999
> CHECKOUT carol 789_Pine_Rd NONE CARD
ERROR EmptyCartException: Cart is empty for customer carol
> STATUS O99
ERROR OrderNotFoundException: No order with id O99
> SHIP O2
ERROR InvalidOrderStateException: Cannot ship order in status CANCELLED
```

The last line is worth noting: `O2` was already cancelled two lines
earlier in the script, and `CancelledState` (like `DeliveredState`)
overrides nothing but `getStatus()`, so `ship()` correctly falls through
to the throwing default — a cancelled order can never be revived into
`SHIPPED`.

---

## 6. Things to try / test yourself

You can edit `test/input/scenario.txt` and re-run with:
```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

1. **Use the other hardcoded coupon.** Add a `CHECKOUT ... SAVE20 CARD`
   line and confirm the total reflects a 20% discount, then try a coupon
   code that isn't `SAVE10`/`SAVE20`/`NONE` (e.g. `SAVE99`) and confirm the
   checkout still succeeds but with `discount = 0.0` — `getOrDefault` in
   `PriceValidationHandler` means an unknown coupon silently applies no
   discount rather than erroring.
2. **Cancel an order while it's still `CONFIRMED` (not yet shipped).**
   `CONFIRM` an order, then `CANCEL` it (skip `SHIP`). Confirm this
   succeeds (unlike cancelling a `SHIPPED` or `DELIVERED` order) and that
   `INVENTORY` shows the stock restored — proving `ConfirmedState`'s
   `cancel()` override works, not just `PlacedState`'s.
3. **Race the stock check against a real decrement.** Add two customers
   each adding the last few units of the same low-stock product to their
   carts, then have both check out back-to-back in the script. Since
   there's no concurrency control here (flagged in the README's "Known
   gaps" — the stock check and the actual decrement are two separate
   steps, a classic "check-then-act" gap), work out by hand whether this
   *particular* single-threaded test harness could ever actually
   oversell — and why a multi-threaded caller could.
4. **Add an item, remove it, then check out with an empty cart.**
   `ADDCART`, then `REMOVECART` the same SKU, then `CHECKOUT`. Confirm you
   get `EmptyCartException`, not a checkout with zero items — proving
   `cart.isEmpty()` genuinely tracks item count, not just "was `ADDCART`
   ever called."
5. **Try to build an `Order` with no items directly.** This isn't
   reachable through `Main`'s command language, but read
   `OrderBuilder.build()`'s two guard checks and work out: what's the
   *only* code path in `EcommerceService.checkout()` that could ever leave
   the builder's `items` list empty by the time `build()` runs? (Hint:
   trace whether `checkout()`'s empty-cart guard at the very top makes
   that path actually unreachable in practice.)
6. **Add a fourth validation handler.** As an exercise (don't need to run
   it), sketch a new `OrderValidationHandler` subclass — e.g. a
   `MaxOrderValueHandler` that rejects anything over some dollar amount —
   and figure out exactly one line you'd need to add to
   `EcommerceService`'s constructor to wire it into the existing chain,
   confirming the README's claim that new checks don't require touching
   `checkout()` itself.
