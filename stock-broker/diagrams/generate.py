# -*- coding: utf-8 -*-
"""Regenerates stock-broker.drawio from docs/tooling/drawio_uml.py.

Run from the repo root: python stock-broker/diagrams/generate.py
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

cells.append(group_title(COL[0], y, "model — accounts & orders"))
y += 34
box, account_id, h1 = uml_box(COL[0], y, 320, "Account",
    attrs=["- accountId: String", "- cashBalance: double", "- holdings: Map<String,Integer>"],
    methods=["+ debit(amount)/credit(amount): void", "+ addHolding(symbol, qty): void",
              "+ removeHolding(symbol, qty): void", "+ getHoldingQuantity(symbol): int"])
cells += box
box, order_id, h2 = uml_box(COL[1], y, 340, "Order",
    attrs=["- orderId/accountId/symbol: String", "- side: OrderSide", "- type: OrderType",
           "- quantity: int", "- limitPrice: Double  // null for MARKET",
           "- status: OrderStatus", "- executionPrice/charges: Double"],
    methods=["+ markExecuted(price, charges): void", "+ markCancelled(): void", "+ getters…"])
cells += box
box, side_id, h3 = uml_box(COL[2], y, 220, "OrderSide", stereotype="enumeration",
    attrs=["BUY", "SELL"], header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
box, type_id, h4 = uml_box(COL[3], y, 220, "OrderType", stereotype="enumeration",
    attrs=["MARKET", "LIMIT"], header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
box, status_id, h5 = uml_box(COL[4], y, 220, "OrderStatus", stereotype="enumeration",
    attrs=["PENDING", "EXECUTED", "CANCELLED"], header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
cells.append(edge(order_id, side_id, "association", "side", exitX="1", exitY="0.15", entryX="0", entryY="0.5"))
cells.append(edge(order_id, type_id, "association", "type", exitX="1", exitY="0.4", entryX="0", entryY="0.5"))
cells.append(edge(order_id, status_id, "association", "status", exitX="1", exitY="0.65", entryX="0", entryY="0.5"))
row1_bottom = y + max(h1, h2, h3, h4, h5)

y = row1_bottom + 70
cells.append(group_title(COL[0], y, "strategy — order execution (Strategy pattern)"))
y += 34
box, ostrat_id, hs0 = uml_box(COL[0], y, 320, "OrderExecutionStrategy", stereotype="interface",
    methods=["+ isExecutable(order, currentPrice): boolean"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, market_id, hs1 = uml_box(COL[1], y, 300, "MarketOrderStrategy",
    methods=["+ isExecutable(order, price): boolean", "  // always true"])
cells += box
box, limit_id, hs2 = uml_box(COL[2], y, 320, "LimitOrderStrategy",
    methods=["+ isExecutable(order, price): boolean", "  // BUY: price<=limit  SELL: price>=limit"])
cells += box
cells.append(edge(market_id, ostrat_id, "realize", exitX="0.5", exitY="0", entryX="0.25", entryY="1"))
cells.append(edge(limit_id, ostrat_id, "realize", exitX="0.5", exitY="0", entryX="0.65", entryY="1"))
row2_bottom = y + max(hs0, hs1, hs2)

y = row2_bottom + 70
cells.append(group_title(COL[0], y, "decorator — charge stacking (Decorator pattern)"))
y += 34
box, charge_id, hd0 = uml_box(COL[0], y, 300, "ChargeCalculator", stereotype="interface",
    methods=["+ totalCharges(tradeValue): double"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, nocharge_id, hd1 = uml_box(COL[1], y, 260, "NoCharge", stereotype="core component",
    methods=["+ totalCharges(tradeValue): double", "  // 0.0"])
cells += box
box, chdecor_id, hd2 = uml_box(COL[2], y, 300, "ChargeDecorator", stereotype="abstract",
    attrs=["# inner: ChargeCalculator"])
cells += box
row3a_bottom = y + max(hd0, hd1, hd2)
y = row3a_bottom + 20
box, brok_id, hd3 = uml_box(COL[1], y, 300, "BrokerageFeeDecorator",
    attrs=["- RATE: double = 0.0025 {static}"],
    methods=["+ totalCharges(v): double", "  // inner.totalCharges(v) + v*RATE"])
cells += box
box, stt_id, hd4 = uml_box(COL[2], y, 300, "SttDecorator",
    attrs=["- RATE: double = 0.001 {static}"],
    methods=["+ totalCharges(v): double", "  // inner.totalCharges(v) + v*RATE"])
cells += box
box, gst_id, hd5 = uml_box(COL[3], y, 300, "GstDecorator",
    attrs=["- RATE: double = 0.0005 {static}"],
    methods=["+ totalCharges(v): double", "  // inner.totalCharges(v) + v*RATE"])
cells += box
cells.append(edge(nocharge_id, charge_id, "realize", exitX="0.5", exitY="0", entryX="0.2", entryY="1"))
cells.append(edge(chdecor_id, charge_id, "realize", exitX="0.5", exitY="0", entryX="0.55", entryY="1"))
cells.append(edge(brok_id, chdecor_id, "inherit", exitX="0.5", exitY="0", entryX="0.3", entryY="1"))
cells.append(edge(stt_id, chdecor_id, "inherit", exitX="0.5", exitY="0", entryX="0.6", entryY="1"))
cells.append(edge(gst_id, chdecor_id, "inherit", exitX="0.5", exitY="0", entryX="0.9", entryY="1"))
cells.append(edge(chdecor_id, charge_id, "aggregation", "inner  1", exitX="0.9", exitY="0", entryX="0.85", entryY="1"))
row3_bottom = y + max(hd3, hd4, hd5)

y = row3_bottom + 70
cells.append(group_title(COL[0], y, "external — third-party feed  +  adapter — price feed translation (Adapter pattern)"))
y += 34
box, extquote_id, ha0 = uml_box(COL[0], y, 300, "ExternalQuote", stereotype="external",
    attrs=["- ticker: String", "- lastTradedPrice: double"],
    methods=["+ getTicker()/getLastTradedPrice()"],
    header_fill="#f5f5f5", header_stroke="#666666", dashed=True)
cells += box
box, extfeed_id, ha1 = uml_box(COL[1], y, 320, "ExternalMarketFeed", stereotype="external",
    attrs=["- latestByTicker: Map<String,ExternalQuote>"],
    methods=["+ publishQuote(ticker, price): void", "+ fetchQuote(ticker): ExternalQuote  // null if unpublished"],
    header_fill="#f5f5f5", header_stroke="#666666", dashed=True)
cells += box
box, pricefeed_id, ha2 = uml_box(COL[2], y, 280, "PriceFeed", stereotype="interface",
    methods=["+ getCurrentPrice(symbol): double"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, adapter_id, ha3 = uml_box(COL[3], y, 320, "ExternalMarketFeedAdapter",
    attrs=["- externalMarketFeed: ExternalMarketFeed"],
    methods=["+ getCurrentPrice(symbol): double", "  // fetchQuote(symbol).getLastTradedPrice()",
              "  // throws NoQuoteAvailableException if null"])
cells += box
cells.append(edge(extfeed_id, extquote_id, "dependency", "returns", exitX="0", exitY="0.5", entryX="1", entryY="0.5"))
cells.append(edge(adapter_id, pricefeed_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.5"))
cells.append(edge(adapter_id, extfeed_id, "association", "«adapts»  1", exitX="0.3", exitY="0", entryX="0.7", entryY="1"))
row4_bottom = y + max(ha0, ha1, ha2, ha3)

y = row4_bottom + 70
cells.append(group_title(COL[0], y, "repository (in-memory)  +  exceptions"))
y += 34
box, acctrepo_id, hr1 = uml_box(COL[0], y, 300, "AccountRepository",
    attrs=["- accountsById: Map<String,Account>"],
    methods=["+ save(account): void", "+ findByAccountId(id): Account"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
box, ordrepo_id, hr2 = uml_box(COL[1], y, 320, "OrderRepository",
    attrs=["- ordersById: Map<String,Order>"],
    methods=["+ save(order): void", "+ findByOrderId(id): Order",
              "+ findPendingBySymbol(symbol): List<Order>", "+ findByAccountId(id): List<Order>"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
box, rte_id, he0 = uml_box(COL[2], y, 240, "RuntimeException", stereotype="java.lang", dashed=True,
    header_fill="#f5f5f5", header_stroke="#666666")
cells += box
box, exc_id, he1 = uml_box(COL[3], y, 340, "AccountNotFoundException\nNoQuoteAvailableException\nInsufficientFundsException", stereotype="…and 4 more",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
cells.append(edge(acctrepo_id, account_id, "aggregation", "0..*", exitX="0.5", exitY="0", entryX="0.15", entryY="1"))
cells.append(edge(ordrepo_id, order_id, "aggregation", "0..*", exitX="0.5", exitY="0", entryX="0.45", entryY="1"))
cells.append(edge(exc_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.5"))
row5_bottom = y + max(hr1, hr2, he0, he1)

y = row5_bottom + 80
cells.append(group_title(COL[0], y, "services — the ONE public entry point"))
y += 34
box, svc_id, hsv = uml_box(COL[0], y, 580, "StockBrokerService",
    attrs=["- accountRepository: AccountRepository", "- orderRepository: OrderRepository",
           "- externalMarketFeed: ExternalMarketFeed", "- priceFeed: PriceFeed",
           "- chargeCalculator: ChargeCalculator", "- orderSequence: AtomicInteger"],
    methods=["+ openAccount(accountId, initialCash): void",
              "+ publishPriceUpdate(symbol, price): void  // rechecks pending orders for that symbol",
              "+ placeOrder(accountId, symbol, side, type, qty, limitPrice): Order",
              "+ cancelOrder(orderId): void",
              "+ getPortfolio(accountId): Account", "+ getOrderHistory(accountId): List<Order>",
              "- executeOrder(order, currentPrice): void", "- resolveStrategy(type): OrderExecutionStrategy"],
    header_fill="#dae8fc", header_stroke="#6c8ebf")
cells += box

cells.append(edge(svc_id, acctrepo_id, "composition", "1", exitX="0.1", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, ordrepo_id, "composition", "1", exitX="0.25", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, adapter_id, "composition", "priceFeed  1", exitX="0.4", exitY="0", entryX="0.7", entryY="1"))
cells.append(edge(svc_id, extfeed_id, "composition", "1", exitX="0.55", exitY="0", entryX="0.3", entryY="1"))
cells.append(edge(svc_id, charge_id, "composition", "chargeCalculator  1", exitX="0.7", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, ostrat_id, "dependency", "resolves per order", exitX="0.85", exitY="0", entryX="0.5", entryY="1"))

PAGE1 = "\n".join(cells)

# ===========================================================================
# PAGE 2: SEQUENCE — PLACE MARKET ORDER (Strategy + Decorator)
# ===========================================================================
cells2 = []
xs = {}
for name, x in [(":Main", 100), ("svc: StockBrokerService", 380), ("strategy: MarketOrderStrategy", 680),
                 ("feed: PriceFeed", 960), ("charges: ChargeCalculator", 1240), ("account: Account", 1520)]:
    box, xx = lifeline(x, name, bottom=1020)
    cells2 += box
    xs[name] = xx

y = 120
cells2.append(msg(xs[":Main"], xs["svc: StockBrokerService"], y, "placeOrder(accountId, \"AAPL\", BUY, MARKET, 10, null)"))
y += 40
cells2.append(selfcall(xs["svc: StockBrokerService"], y, "validate account exists, quantity > 0", loop_w=110, loop_h=22))
y += 50
cells2.append(msg(xs["svc: StockBrokerService"], xs["strategy: MarketOrderStrategy"], y, "resolveStrategy(MARKET) -> new MarketOrderStrategy()"))
y += 44
cells2.append(msg(xs["svc: StockBrokerService"], xs["feed: PriceFeed"], y, "getCurrentPrice(\"AAPL\")   // via ExternalMarketFeedAdapter"))
y += 40
cells2.append(msg(xs["feed: PriceFeed"], xs["svc: StockBrokerService"], y, "return currentPrice", kind="return"))
y += 44
cells2.append(msg(xs["svc: StockBrokerService"], xs["strategy: MarketOrderStrategy"], y, "isExecutable(order, currentPrice)"))
y += 40
cells2.append(msg(xs["strategy: MarketOrderStrategy"], xs["svc: StockBrokerService"], y, "return true", kind="return"))
y += 50
cells2.append(selfcall(xs["svc: StockBrokerService"], y, "executeOrder(order, currentPrice)", loop_w=90, loop_h=22))
y += 50
cells2.append(msg(xs["svc: StockBrokerService"], xs["charges: ChargeCalculator"], y, "totalCharges(tradeValue)   // GstDecorator -> SttDecorator -> BrokerageFeeDecorator -> NoCharge"))
y += 40
cells2.append(msg(xs["charges: ChargeCalculator"], xs["svc: StockBrokerService"], y, "return charges  (sum of all three layers)", kind="return"))
y += 44
cells2.append(frame(xs["svc: StockBrokerService"] - 60, xs["account: Account"] - xs["svc: StockBrokerService"] + 160, y, 90,
                     "alt  [tradeValue + charges > account.getCashBalance()]"))
cells2.append(selfcall(xs["svc: StockBrokerService"], y + 25, "throw InsufficientFundsException", loop_w=100, loop_h=20))
cells2.append(divider(xs["svc: StockBrokerService"] - 60, xs["account: Account"] - xs["svc: StockBrokerService"] + 160, y + 55, "[else: funds ok]"))
y += 100
cells2.append(msg(xs["svc: StockBrokerService"], xs["account: Account"], y, "debit(tradeValue + charges)"))
y += 40
cells2.append(msg(xs["svc: StockBrokerService"], xs["account: Account"], y, "addHolding(\"AAPL\", 10)"))
y += 44
cells2.append(selfcall(xs["svc: StockBrokerService"], y, "order.markExecuted(currentPrice, charges)", loop_w=110, loop_h=22))
y += 50
cells2.append(msg(xs["svc: StockBrokerService"], xs[":Main"], y, "return order  (status = EXECUTED)", kind="return"))

PAGE2 = "\n".join(cells2)

# ===========================================================================
# PAGE 3: SEQUENCE — PRICE UPDATE TRIGGERS A PENDING LIMIT ORDER
# ===========================================================================
cells3 = []
xs = {}
for name, x in [(":Main", 120), ("svc: StockBrokerService", 420), ("external: ExternalMarketFeed", 740),
                 ("feed: PriceFeed", 1040), ("strategy: LimitOrderStrategy", 1340), ("order: Order", 1600)]:
    box, xx = lifeline(x, name, bottom=900)
    cells3 += box
    xs[name] = xx

y = 120
cells3.append(msg(xs[":Main"], xs["svc: StockBrokerService"], y, "publishPriceUpdate(\"AAPL\", 138.00)"))
y += 40
cells3.append(msg(xs["svc: StockBrokerService"], xs["external: ExternalMarketFeed"], y, "publishQuote(\"AAPL\", 138.00)   // the \"tick\" arrives"))
y += 44
cells3.append(msg(xs["svc: StockBrokerService"], xs["feed: PriceFeed"], y, "getCurrentPrice(\"AAPL\")   // adapter reads it straight back"))
y += 40
cells3.append(msg(xs["feed: PriceFeed"], xs["svc: StockBrokerService"], y, "return 138.00", kind="return"))
y += 50
cells3.append(frame(xs["svc: StockBrokerService"] - 40, xs["order: Order"] - xs["svc: StockBrokerService"] + 140, y, 220,
                     "loop  [for each PENDING order on \"AAPL\"]"))
y += 34
cells3.append(msg(xs["svc: StockBrokerService"], xs["strategy: LimitOrderStrategy"], y, "isExecutable(order, 138.00)   // bob's BUY limit=140"))
y += 40
cells3.append(msg(xs["strategy: LimitOrderStrategy"], xs["svc: StockBrokerService"], y, "return 138.00 <= 140  -> true", kind="return"))
y += 44
cells3.append(frame(xs["svc: StockBrokerService"] - 20, xs["order: Order"] - xs["svc: StockBrokerService"] + 120, y, 90,
                     "alt  [executeOrder() throws Insufficient*]"))
cells3.append(note(xs["svc: StockBrokerService"] + 20, y + 22, 280, "caught locally -- order stays PENDING,\nloop continues to the next order"))
cells3.append(divider(xs["svc: StockBrokerService"] - 20, xs["order: Order"] - xs["svc: StockBrokerService"] + 120, y + 58, "[else: succeeds]"))
y += 100
cells3.append(selfcall(xs["svc: StockBrokerService"], y, "executeOrder(order, 138.00)   // same path as page 2", loop_w=150, loop_h=22))
y += 60
cells3.append(note(xs["order: Order"] + 20, y, 260, "bob's O3 flips PENDING -> EXECUTED;\nalice's pending SELL@160 is also checked\nthis pass but 138 < 160, stays PENDING"))
y += 90
cells3.append(msg(xs["svc: StockBrokerService"], xs[":Main"], y, "return", kind="return"))

PAGE3 = "\n".join(cells3)

# ===========================================================================
outpath = os.path.join(os.path.dirname(__file__), "stock-broker.drawio")
write_mxfile([
    page("classDiagram", "1 - Class Diagram", PAGE1, w=2000, h=2200),
    page("seqPlaceOrder", "2 - Sequence - Place Market Order", PAGE2, w=1900, h=1050),
    page("seqPriceUpdate", "3 - Sequence - Price Update Triggers Limit Order", PAGE3, w=1900, h=950),
], outpath)
validate(outpath)
print("wrote", outpath)
