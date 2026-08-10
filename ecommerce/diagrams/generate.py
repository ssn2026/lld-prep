# -*- coding: utf-8 -*-
"""Regenerates ecommerce.drawio from docs/tooling/drawio_uml.py.

Run from the repo root: python ecommerce/diagrams/generate.py
Copied from parking-lot/diagrams/generate.py's structure per CLAUDE.md --
only supplies data (class fields/methods, edges, sequence messages); all
escaping/geometry logic lives in the shared module.
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "docs", "tooling"))
from drawio_uml import (uml_box, group_title, edge, lifeline, msg, selfcall,
                         frame, divider, note, page, write_mxfile, validate)

# ===========================================================================
# PAGE 1: CLASS DIAGRAM
# ===========================================================================
cells = []
COL = [40, 400, 760, 1120, 1480]
y = 20

cells.append(group_title(COL[0], y, "model — catalog & cart"))
y += 34
box, product_id, h1 = uml_box(COL[0], y, 320, "Product",
    attrs=["- sku: String", "- name: String", "- price: double", "- stockQuantity: int"],
    methods=["+ getSku()/getName()/getPrice()", "+ getStockQuantity(): int",
             "+ decreaseStock(qty: int): void", "+ increaseStock(qty: int): void"])
cells += box
box, cartitem_id, h2 = uml_box(COL[1], y, 300, "CartItem",
    attrs=["- product: Product", "- quantity: int"],
    methods=["+ getProduct(): Product", "+ getQuantity(): int"])
cells += box
box, cart_id, h3 = uml_box(COL[2], y, 340, "Cart",
    attrs=["- customerId: String", "- itemsBySku: Map<String,CartItem>"],
    methods=["+ addItem(product, quantity): void", "+ removeItem(sku): void",
             "+ getItems(): List<CartItem>", "+ isEmpty(): boolean", "+ clear(): void"])
cells += box
cells.append(edge(cart_id, cartitem_id, "composition", "itemsBySku  0..*",
                   exitX="0.5", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(cartitem_id, product_id, "association", "product  1",
                   exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
row1_bottom = y + max(h1, h2, h3)

y = row1_bottom + 70
cells.append(group_title(COL[0], y, "model — order"))
y += 34
box, orderitem_id, h4 = uml_box(COL[0], y, 320, "OrderItem",
    attrs=["- product: Product", "- quantity: int", "- priceAtPurchase: double"],
    methods=["+ getProduct()/getQuantity()", "+ getPriceAtPurchase(): double",
             "+ getSubtotal(): double"])
cells += box
box, order_id, h5 = uml_box(COL[1], y, 360, "Order",
    attrs=["- orderId: String", "- customerId: String", "- items: List<OrderItem>",
           "- shippingAddress: String", "- totalAmount: double", "- couponCode: String",
           "- state: OrderState"],
    methods=["+ confirm()/ship()/deliver()/cancel(): void", "  // delegate to state.<transition>()",
              "+ getStatus(): OrderStatus", "+ getOrderId()/getCustomerId()/getItems()/…"])
cells += box
box, ostatus_id, h6 = uml_box(COL[2], y, 300, "OrderStatus", stereotype="enumeration",
    attrs=["PLACED", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"],
    header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
cells.append(edge(order_id, orderitem_id, "composition", "items  1..*",
                   exitX="0", exitY="0.3", entryX="1", entryY="0.4"))
cells.append(edge(orderitem_id, product_id, "association", "product  1",
                   exitX="0.5", exitY="0", entryX="0.15", entryY="1"))
row2_bottom = y + max(h4, h5, h6)

y = row2_bottom + 70
cells.append(group_title(COL[0], y, "state — order lifecycle (State pattern, held on Order)"))
y += 34
box, ostate_id, hs0 = uml_box(COL[0], y, 320, "OrderState", stereotype="interface",
    methods=["+ getStatus(): OrderStatus", "+ confirm()/ship()/deliver()/cancel(): OrderState",
              "  // defaults throw InvalidOrderStateException;", "  // each concrete state overrides only its legal moves"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, placed_id, hs1 = uml_box(COL[1], y, 260, "PlacedState", stereotype="singleton",
    methods=["+ confirm(): OrderState", "+ cancel(): OrderState"])
cells += box
box, confirmed_id, hs2 = uml_box(COL[2], y, 260, "ConfirmedState", stereotype="singleton",
    methods=["+ ship(): OrderState", "+ cancel(): OrderState"])
cells += box
box, shipped_id, hs3 = uml_box(COL[3], y, 260, "ShippedState", stereotype="singleton",
    methods=["+ deliver(): OrderState", "  // no cancel() override"])
cells += box
box, delivered_id, hs4 = uml_box(COL[4], y, 260, "DeliveredState", stereotype="singleton",
    methods=["  // terminal: all defaults throw"])
cells += box
row3a_bottom = y + max(hs0, hs1, hs2, hs3, hs4)
y = row3a_bottom + 20
box, cancelled_id, hs5 = uml_box(COL[1], y, 260, "CancelledState", stereotype="singleton",
    methods=["  // terminal: all defaults throw"])
cells += box
cells.append(edge(placed_id, ostate_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.2"))
cells.append(edge(confirmed_id, ostate_id, "realize", exitX="0.5", exitY="0", entryX="0.6", entryY="1"))
cells.append(edge(shipped_id, ostate_id, "realize", exitX="0.5", exitY="0", entryX="0.8", entryY="1"))
cells.append(edge(delivered_id, ostate_id, "realize", exitX="0.5", exitY="0", entryX="1", entryY="0.35"))
cells.append(edge(cancelled_id, ostate_id, "realize", exitX="1", exitY="0", entryX="1", entryY="0.5"))
cells.append(edge(placed_id, confirmed_id, "dependency", "confirm() →", exitX="1", exitY="0.3", entryX="0", entryY="0.3"))
cells.append(edge(confirmed_id, shipped_id, "dependency", "ship() →", exitX="1", exitY="0.3", entryX="0", entryY="0.3"))
cells.append(edge(shipped_id, delivered_id, "dependency", "deliver() →", exitX="1", exitY="0.3", entryX="0", entryY="0.3"))
cells.append(edge(placed_id, cancelled_id, "dependency", "cancel() →", exitX="0.3", exitY="1", entryX="0.3", entryY="0"))
cells.append(edge(confirmed_id, cancelled_id, "dependency", "cancel() →", exitX="0.3", exitY="1", entryX="0.9", entryY="0.3"))
cells.append(edge(order_id, ostate_id, "association", "state  1", exitX="0.2", exitY="1", entryX="0.5", entryY="0"))
row3_bottom = y + hs5

y = row3_bottom + 70
cells.append(group_title(COL[0], y, "chain — checkout validation pipeline (Chain of Responsibility)"))
y += 34
box, handler_id, hc0 = uml_box(COL[0], y, 340, "OrderValidationHandler", stereotype="abstract",
    attrs=["- next: OrderValidationHandler"],
    methods=["+ linkWith(next): OrderValidationHandler", "+ handle(context): void  {final}",
              "  // validate(context), then next.handle(context)", "# validate(context): void {abstract}"])
cells += box
box, stock_id, hc1 = uml_box(COL[1], y, 300, "StockAvailabilityHandler",
    methods=["# validate(context): void", "  // throws OutOfStockException"])
cells += box
box, price_id, hc2 = uml_box(COL[2], y, 300, "PriceValidationHandler",
    attrs=["- COUPON_DISCOUNTS: Map<String,Double> {static}"],
    methods=["# validate(context): void", "  // recomputes total, applies coupon"])
cells += box
box, pay_id, hc3 = uml_box(COL[3], y, 300, "PaymentAuthorizationHandler",
    methods=["# validate(context): void", "  // throws PaymentDeclinedException"])
cells += box
box, ctx_id, hc4 = uml_box(COL[4], y, 320, "OrderValidationContext",
    attrs=["- cart: Cart", "- productRepository: ProductRepository", "- couponCode: String",
           "- paymentMethod: String", "- computedTotal: double"],
    methods=["+ getters/setComputedTotal(double)"])
cells += box
cells.append(edge(stock_id, handler_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.25"))
cells.append(edge(price_id, handler_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.5"))
cells.append(edge(pay_id, handler_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.75"))
cells.append(edge(handler_id, ctx_id, "dependency", "handle(context)", exitX="1", exitY="0.7", entryX="0", entryY="0.5"))
row4_bottom = y + max(hc0, hc1, hc2, hc3, hc4)

y = row4_bottom + 70
cells.append(group_title(COL[0], y, "builder — order construction (Builder pattern)"))
y += 34
box, builder_id, hb1 = uml_box(COL[0], y, 340, "OrderBuilder",
    attrs=["- orderId: String", "- customerId: String", "- items: List<OrderItem>",
           "- shippingAddress: String", "- totalAmount: double", "- couponCode: String"],
    methods=["+ orderId(id)/customerId(id): OrderBuilder", "+ addItem(product, qty, price): OrderBuilder",
              "+ shippingAddress(addr)/totalAmount(amt)/couponCode(code): OrderBuilder",
              "+ build(): Order  // validates required fields"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
cells.append(edge(builder_id, order_id, "dependency", "«creates»", exitX="1", exitY="0.3", entryX="0", entryY="0.9"))
row5_bottom = y + hb1

y = row5_bottom + 70
cells.append(group_title(COL[0], y, "repository (in-memory)"))
y += 34
box, prodrepo_id, hr1 = uml_box(COL[0], y, 340, "ProductRepository",
    attrs=["- productsBySku: Map<String,Product>"],
    methods=["+ save(product): void", "+ findBySku(sku): Product  // throws ProductNotFoundException",
              "+ getAllProducts(): Collection<Product>"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
box, ordrepo_id, hr2 = uml_box(COL[1], y, 340, "OrderRepository",
    attrs=["- ordersById: Map<String,Order>"],
    methods=["+ save(order): void", "+ findById(orderId): Order  // throws OrderNotFoundException"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
cells.append(edge(prodrepo_id, product_id, "aggregation", "0..*", exitX="0.5", exitY="0", entryX="0.3", entryY="1"))
cells.append(edge(ordrepo_id, order_id, "aggregation", "0..*", exitX="0.5", exitY="0", entryX="0.75", entryY="1"))
row6_bottom = y + max(hr1, hr2)

y = row6_bottom + 70
cells.append(group_title(COL[0], y, "exceptions"))
y += 34
box, rte_id, he0 = uml_box(COL[0], y, 260, "RuntimeException", stereotype="java.lang", dashed=True,
    header_fill="#f5f5f5", header_stroke="#666666")
cells += box
box, pnf_id, he1 = uml_box(COL[1], y, 280, "ProductNotFoundException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, oos_id, he2 = uml_box(COL[2], y, 280, "OutOfStockException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, ios_id, he3 = uml_box(COL[3], y, 280, "InvalidOrderStateException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
row7a_bottom = y + max(he0, he1, he2, he3)
y = row7a_bottom + 20
box, pd_id, he4 = uml_box(COL[1], y, 280, "PaymentDeclinedException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, onf_id, he5 = uml_box(COL[2], y, 280, "OrderNotFoundException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, ec_id, he6 = uml_box(COL[3], y, 280, "EmptyCartException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
for eid in (pnf_id, oos_id, ios_id):
    cells.append(edge(eid, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
for eid in (pd_id, onf_id, ec_id):
    cells.append(edge(eid, rte_id, "inherit", exitX="0", exitY="0", entryX="1", entryY="0.7"))
row7_bottom = y + max(he4, he5, he6)

y = row7_bottom + 80
cells.append(group_title(COL[0], y, "services — the ONE public entry point"))
y += 34
box, svc_id, hsv = uml_box(COL[0], y, 560, "EcommerceService",
    attrs=["- productRepository: ProductRepository", "- orderRepository: OrderRepository",
           "- cartsByCustomerId: Map<String,Cart>", "- orderSequence: AtomicInteger",
           "- validationChain: OrderValidationHandler"],
    methods=["+ addProduct(sku, name, price, stock): void",
              "+ addToCart(customerId, sku, qty): void", "+ removeFromCart(customerId, sku): void",
              "+ viewCart(customerId): Cart",
              "+ checkout(customerId, address, coupon, paymentMethod): Order",
              "+ confirmOrder(orderId)/shipOrder(orderId)/deliverOrder(orderId): void",
              "+ cancelOrder(orderId): void  // restocks items",
              "+ getOrderStatus(orderId): OrderStatus", "+ getInventoryReport(): String",
              "- getCart(customerId): Cart"],
    header_fill="#dae8fc", header_stroke="#6c8ebf")
cells += box

cells.append(edge(svc_id, prodrepo_id, "composition", "productRepository  1", exitX="0.1", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, ordrepo_id, "composition", "orderRepository  1", exitX="0.25", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, cart_id, "composition", "cartsByCustomerId  0..*", exitX="0.4", exitY="0", entryX="0.7", entryY="1"))
cells.append(edge(svc_id, handler_id, "composition", "validationChain  1", exitX="0.6", exitY="0", entryX="0.3", entryY="1"))
cells.append(edge(svc_id, builder_id, "dependency", "uses per checkout", exitX="0.75", exitY="0", entryX="0.7", entryY="1"))
cells.append(edge(svc_id, ostate_id, "dependency", "triggers via Order", exitX="0.9", exitY="0", entryX="0.9", entryY="1"))

PAGE1 = "\n".join(cells)

# ===========================================================================
# PAGE 2: SEQUENCE — CHECKOUT (Chain of Responsibility + Builder)
# ===========================================================================
cells2 = []
xs = {}
for name, x in [(":Main", 100), ("svc: EcommerceService", 380), ("chain: OrderValidationHandler", 700),
                 ("ctx: OrderValidationContext", 1020), ("builder: OrderBuilder", 1320), ("orderRepo: OrderRepository", 1600)]:
    box, xx = lifeline(x, name, bottom=1080)
    cells2 += box
    xs[name] = xx

y = 120
cells2.append(msg(xs[":Main"], xs["svc: EcommerceService"], y, "checkout(customerId, address, couponCode, paymentMethod)"))
y += 40
cells2.append(frame(xs["svc: EcommerceService"] - 60, xs["ctx: OrderValidationContext"] - xs["svc: EcommerceService"] + 160, y, 90,
                     "alt  [cart.isEmpty()]"))
cells2.append(selfcall(xs["svc: EcommerceService"], y + 25, "throw EmptyCartException", loop_w=70, loop_h=20))
cells2.append(divider(xs["svc: EcommerceService"] - 60, xs["ctx: OrderValidationContext"] - xs["svc: EcommerceService"] + 160, y + 55, "[else: cart has items]"))
y += 100
cells2.append(msg(xs["svc: EcommerceService"], xs["ctx: OrderValidationContext"], y, "«create» new OrderValidationContext(cart, productRepository, coupon, paymentMethod)", kind="create"))
y += 50
cells2.append(msg(xs["svc: EcommerceService"], xs["chain: OrderValidationHandler"], y, "handle(context)   // head = StockAvailabilityHandler"))
y += 40
cells2.append(frame(xs["svc: EcommerceService"] - 40, xs["ctx: OrderValidationContext"] - xs["svc: EcommerceService"] + 140, y, 190,
                     "loop  [3 links: Stock → Price/Coupon → PaymentAuth]"))
y += 34
cells2.append(msg(xs["chain: OrderValidationHandler"], xs["ctx: OrderValidationContext"], y, "validate(context)   // reads cart/coupon, may write computedTotal"))
y += 40
cells2.append(selfcall(xs["chain: OrderValidationHandler"], y, "if (next != null) next.handle(context)", loop_w=100, loop_h=22))
y += 34
cells2.append(note(xs["chain: OrderValidationHandler"] + 30, y, 300,
                    "any handler may throw (OutOfStock /\nPaymentDeclined) — checkout aborts,\nno stock or cart mutation has happened yet"))
y += 90
cells2.append(msg(xs["chain: OrderValidationHandler"], xs["svc: EcommerceService"], y, "return  (chain completed without throwing)", kind="return"))
y += 60
cells2.append(frame(xs["svc: EcommerceService"] - 40, xs["orderRepo: OrderRepository"] - xs["svc: EcommerceService"] + 140, y, 150,
                     "loop  [for each CartItem]"))
y += 34
cells2.append(selfcall(xs["svc: EcommerceService"], y, "product.decreaseStock(quantity)", loop_w=90, loop_h=22))
y += 50
cells2.append(msg(xs["svc: EcommerceService"], xs["builder: OrderBuilder"], y, "addItem(product, quantity, product.getPrice())"))
y += 60
cells2.append(msg(xs["svc: EcommerceService"], xs["builder: OrderBuilder"], y, "build()   // orderId, customerId, address, computedTotal, coupon"))
y += 40
cells2.append(msg(xs["builder: OrderBuilder"], xs["svc: EcommerceService"], y, "return order  (state = PlacedState.INSTANCE)", kind="return"))
y += 50
cells2.append(msg(xs["svc: EcommerceService"], xs["orderRepo: OrderRepository"], y, "save(order)"))
y += 44
cells2.append(selfcall(xs["svc: EcommerceService"], y, "cart.clear()", loop_w=60, loop_h=20))
y += 50
cells2.append(msg(xs["svc: EcommerceService"], xs[":Main"], y, "return order", kind="return"))

PAGE2 = "\n".join(cells2)

# ===========================================================================
# PAGE 3: SEQUENCE — ORDER LIFECYCLE TRANSITIONS (State pattern)
# ===========================================================================
cells3 = []
xs = {}
for name, x in [(":Main", 120), ("svc: EcommerceService", 420), ("orderRepo: OrderRepository", 740),
                 ("order: Order", 1040), ("state: OrderState", 1340)]:
    box, xx = lifeline(x, name, bottom=760)
    cells3 += box
    xs[name] = xx

y = 120
cells3.append(msg(xs[":Main"], xs["svc: EcommerceService"], y, "confirmOrder(orderId)"))
y += 40
cells3.append(msg(xs["svc: EcommerceService"], xs["orderRepo: OrderRepository"], y, "findById(orderId)"))
y += 40
cells3.append(msg(xs["orderRepo: OrderRepository"], xs["svc: EcommerceService"], y, "return order", kind="return"))
y += 44
cells3.append(msg(xs["svc: EcommerceService"], xs["order: Order"], y, "confirm()"))
y += 40
cells3.append(msg(xs["order: Order"], xs["state: OrderState"], y, "state.confirm()   // current state = PlacedState.INSTANCE"))
y += 44
cells3.append(msg(xs["state: OrderState"], xs["order: Order"], y, "return ConfirmedState.INSTANCE", kind="return"))
y += 30
cells3.append(note(xs["order: Order"] + 20, y, 280, "Order.state = result;\nnext confirm()/ship() call now\ndispatches on ConfirmedState"))
y += 70
cells3.append(msg(xs[":Main"], xs["svc: EcommerceService"], y, "shipOrder(orderId)"))
y += 40
cells3.append(msg(xs["svc: EcommerceService"], xs["order: Order"], y, "ship()"))
y += 40
cells3.append(msg(xs["order: Order"], xs["state: OrderState"], y, "state.ship()   // current state = ConfirmedState.INSTANCE"))
y += 44
cells3.append(msg(xs["state: OrderState"], xs["order: Order"], y, "return ShippedState.INSTANCE", kind="return"))
y += 60
cells3.append(frame(xs["svc: EcommerceService"] - 60, xs["state: OrderState"] - xs["svc: EcommerceService"] + 160, y, 90,
                     "alt  [caller instead calls cancel() from ShippedState]"))
cells3.append(selfcall(xs["order: Order"], y + 25, "throw InvalidOrderStateException", loop_w=90, loop_h=20))
cells3.append(divider(xs["svc: EcommerceService"] - 60, xs["state: OrderState"] - xs["svc: EcommerceService"] + 160, y + 55,
                       "[ShippedState has no cancel() override → interface default throws]"))

PAGE3 = "\n".join(cells3)

# ===========================================================================
# PAGE 4: SEQUENCE — CANCEL ORDER (State + restock)
# ===========================================================================
cells4 = []
xs = {}
for name, x in [(":Main", 120), ("svc: EcommerceService", 420), ("orderRepo: OrderRepository", 720),
                 ("order: Order", 1000), ("productRepo: ProductRepository", 1300)]:
    box, xx = lifeline(x, name, bottom=680)
    cells4 += box
    xs[name] = xx

y = 120
cells4.append(msg(xs[":Main"], xs["svc: EcommerceService"], y, "cancelOrder(orderId)"))
y += 40
cells4.append(msg(xs["svc: EcommerceService"], xs["orderRepo: OrderRepository"], y, "findById(orderId)"))
y += 40
cells4.append(msg(xs["orderRepo: OrderRepository"], xs["svc: EcommerceService"], y, "return order", kind="return"))
y += 44
cells4.append(msg(xs["svc: EcommerceService"], xs["order: Order"], y, "cancel()   // throws InvalidOrderStateException if illegal (see page 3)"))
y += 44
cells4.append(msg(xs["order: Order"], xs["svc: EcommerceService"], y, "return  (state = CancelledState.INSTANCE)", kind="return"))
y += 60
cells4.append(frame(xs["svc: EcommerceService"] - 40, xs["productRepo: ProductRepository"] - xs["svc: EcommerceService"] + 140, y, 150,
                     "loop  [for each OrderItem in order.getItems()]"))
y += 34
cells4.append(msg(xs["svc: EcommerceService"], xs["productRepo: ProductRepository"], y, "findBySku(item.getProduct().getSku())"))
y += 40
cells4.append(msg(xs["productRepo: ProductRepository"], xs["svc: EcommerceService"], y, "return product", kind="return"))
y += 34
cells4.append(selfcall(xs["svc: EcommerceService"], y, "product.increaseStock(item.getQuantity())", loop_w=110, loop_h=22))
y += 90
cells4.append(msg(xs["svc: EcommerceService"], xs[":Main"], y, "return", kind="return"))

PAGE4 = "\n".join(cells4)

# ===========================================================================
outpath = os.path.join(os.path.dirname(__file__), "ecommerce.drawio")
write_mxfile([
    page("classDiagram", "1 - Class Diagram", PAGE1, w=2000, h=2900),
    page("seqCheckout", "2 - Sequence - Checkout", PAGE2, w=1900, h=1150),
    page("seqLifecycle", "3 - Sequence - Order Lifecycle (State)", PAGE3, w=1700, h=850),
    page("seqCancel", "4 - Sequence - Cancel Order (restock)", PAGE4, w=1600, h=750),
], outpath)
validate(outpath)
print("wrote", outpath)
