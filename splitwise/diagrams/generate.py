# -*- coding: utf-8 -*-
"""Regenerates splitwise.drawio from docs/tooling/drawio_uml.py.

Run from the repo root: python splitwise/diagrams/generate.py
Copied from parking-lot/diagrams/generate.py, the template these scripts
share — only supplies data (class fields/methods, edges, sequence messages);
all escaping/geometry logic lives in the shared module.
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

cells.append(group_title(COL[0], y, "model"))
y += 34
box, user_id, h1 = uml_box(COL[0], y, 300, "User",
    attrs=["- id: String", "- name: String", "- email: String"],
    methods=["+ getId(): String", "+ getName(): String", "+ getEmail(): String"])
cells += box
box, split_id, h2 = uml_box(COL[1], y, 280, "Split",
    attrs=["- user: User", "- amount: double"],
    methods=["+ getUser(): User", "+ getAmount(): double"])
cells += box
box, stype_id, h3 = uml_box(COL[2], y, 240, "SplitType", stereotype="enumeration",
    attrs=["EQUAL", "EXACT", "PERCENT"], header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
box, expense_id, h4 = uml_box(COL[3], y, 340, "Expense",
    attrs=["- id: String", "- description: String", "- amount: double",
           "- paidBy: User", "- splitType: SplitType", "- splits: List<Split>"],
    methods=["+ getId()/getDescription()/getAmount()", "+ getPaidBy(): User",
              "+ getSplitType(): SplitType", "+ getSplits(): List<Split>"])
cells += box
cells.append(edge(split_id, user_id, "association", "user  1", exitX="0", exitY="0.5", entryX="1", entryY="0.5"))
cells.append(edge(expense_id, user_id, "association", "paidBy  1", exitX="0.15", exitY="0", entryX="1", entryY="0.15"))
cells.append(edge(expense_id, split_id, "composition", "splits  1..*", exitX="0", exitY="0.5", entryX="1", entryY="0.7"))
cells.append(edge(expense_id, stype_id, "association", "splitType", exitX="0.4", exitY="0", entryX="0.5", entryY="1"))
row1_bottom = y + max(h1, h2, h3, h4)

y = row1_bottom + 70
cells.append(group_title(COL[0], y, "strategy — split calculation (chosen per addExpense() call)"))
y += 34
box, strat_id, hs1 = uml_box(COL[0], y, 320, "SplitStrategy", stereotype="interface",
    methods=["+ computeSplits(totalAmount: double,", "    participants: List<User>,", "    shareInputs: Map<String,Double>): List<Split>"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, eq_id, hs2 = uml_box(COL[1], y, 300, "EqualSplitStrategy",
    methods=["+ computeSplits(...): List<Split>", "  // amount / n, remainder to last"])
cells += box
box, ex_id, hs3 = uml_box(COL[2], y, 300, "ExactSplitStrategy",
    methods=["+ computeSplits(...): List<Split>", "  // validates sum == totalAmount"])
cells += box
box, pc_id, hs4 = uml_box(COL[3], y, 300, "PercentSplitStrategy",
    methods=["+ computeSplits(...): List<Split>", "  // validates percentages sum to 100"])
cells += box
cells.append(edge(eq_id, strat_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
cells.append(edge(ex_id, strat_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.5"))
cells.append(edge(pc_id, strat_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.7"))
row2_bottom = y + max(hs1, hs2, hs3, hs4)

y = row2_bottom + 70
cells.append(group_title(COL[0], y, "factory & observer"))
y += 34
box, factory_id, hf1 = uml_box(COL[0], y, 320, "SplitStrategyFactory", stereotype="static factory",
    methods=["+ getStrategy(type: SplitType):", "    SplitStrategy {static}"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
box, obs_id, hf2 = uml_box(COL[1], y, 300, "ExpenseObserver", stereotype="interface",
    methods=["+ onExpenseAdded(expense): void", "+ onSettlement(payer, payee, amount): void"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, notif_id, hf3 = uml_box(COL[2], y, 320, "ConsoleNotifier",
    attrs=["- sink: Consumer<String>"],
    methods=["+ onExpenseAdded(expense): void", "+ onSettlement(payer, payee, amount): void"])
cells += box
cells.append(edge(factory_id, strat_id, "dependency", "«creates»", exitX="1", exitY="0.3", entryX="0", entryY="0.95"))
cells.append(edge(notif_id, obs_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.5"))
row3_bottom = y + max(hf1, hf2, hf3)

y = row3_bottom + 70
cells.append(group_title(COL[0], y, "repository"))
y += 34
box, urepo_id, hr1 = uml_box(COL[0], y, 320, "UserRepository",
    attrs=["- usersById: Map<String,User>"],
    methods=["+ save(user): User", "+ findById(userId): User", "+ findAll(): Collection<User>"])
cells += box
box, brepo_id, hr2 = uml_box(COL[1], y, 380, "BalanceSheetRepository",
    attrs=["- balances: Map<String,Map<String,Double>>", "  // balances[A][B] = amount B owes A"],
    methods=["+ adjust(creditor, debtor, amount): void", "+ getBalance(a, b): double",
              "+ getBalancesFor(userId): Map<String,Double>"])
cells += box
cells.append(edge(urepo_id, user_id, "aggregation", "usersById", exitX="0.5", exitY="0", entryX="0.3", entryY="1"))
cells.append(edge(brepo_id, user_id, "dependency", "keyed by id", exitX="0.5", exitY="0", entryX="0.7", entryY="1"))
row4_bottom = y + max(hr1, hr2)

y = row4_bottom + 70
cells.append(group_title(COL[0], y, "exceptions"))
y += 34
box, rte_id, he1 = uml_box(COL[0], y, 260, "RuntimeException", stereotype="java.lang", dashed=True,
    header_fill="#f5f5f5", header_stroke="#666666")
cells += box
box, unf_id, he2 = uml_box(COL[1], y, 300, "UserNotFoundException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, isp_id, he3 = uml_box(COL[2], y, 300, "InvalidSplitException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
cells.append(edge(unf_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
cells.append(edge(isp_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.7"))
row5_bottom = y + max(he1, he2, he3)

y = row5_bottom + 80
cells.append(group_title(COL[0], y, "services — the ONE public entry point"))
y += 34
box, svc_id, hsv = uml_box(COL[0], y, 520, "SplitwiseService",
    attrs=["- userRepository: UserRepository", "- balanceSheet: BalanceSheetRepository",
           "- observers: List<ExpenseObserver>"],
    methods=["+ addUser(name, email): User", "+ addObserver(observer): void",
              "+ addExpense(description, amount, paidByUserId,", "    splitType, participantUserIds, shareInputs): Expense",
              "+ settleUp(payerUserId, payeeUserId, amount): void",
              "+ showBalance(userId1, userId2): String", "+ showBalancesFor(userId): String",
              "+ showAllBalances(): String", "- formatBalance(a, b, amountBOwesA): String"],
    header_fill="#dae8fc", header_stroke="#6c8ebf")
cells += box

cells.append(edge(svc_id, urepo_id, "composition", "userRepository  1", exitX="0.1", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, brepo_id, "composition", "balanceSheet  1", exitX="0.25", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, obs_id, "aggregation", "observers  0..*", exitX="0.4", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, factory_id, "dependency", "uses", exitX="0.55", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, expense_id, "dependency", "creates", exitX="0.7", exitY="0", entryX="0.5", entryY="1"))

PAGE1 = "\n".join(cells)

# ===========================================================================
# PAGE 2: SEQUENCE — ADD EXPENSE
# ===========================================================================
cells2 = []
xs = {}
for name, x in [(":Main", 100), ("service: SplitwiseService", 380), ("factory: SplitStrategyFactory", 700),
                 ("strategy: SplitStrategy", 1000), ("balanceSheet: BalanceSheetRepository", 1300),
                 ("notifier: ConsoleNotifier", 1620)]:
    box, xx = lifeline(x, name, bottom=760)
    cells2 += box
    xs[name] = xx

y = 120
cells2.append(msg(xs[":Main"], xs["service: SplitwiseService"], y,
                   "addExpense(description, amount, paidByUserId, splitType, participantIds, shareInputs)"))
y += 50
cells2.append(selfcall(xs["service: SplitwiseService"], y, "userRepository.findById(...) for payer + each participant", loop_w=110, loop_h=24))
y += 60
cells2.append(msg(xs["service: SplitwiseService"], xs["factory: SplitStrategyFactory"], y, "getStrategy(splitType)"))
y += 40
cells2.append(msg(xs["factory: SplitStrategyFactory"], xs["service: SplitwiseService"], y, "return strategy", kind="return"))
y += 50
cells2.append(msg(xs["service: SplitwiseService"], xs["strategy: SplitStrategy"], y, "computeSplits(amount, participants, shareInputs)"))
y += 34
cells2.append(note(xs["strategy: SplitStrategy"] + 20, y, 260,
                    "EQUAL divides evenly; EXACT/PERCENT\nvalidate the shareInputs sum and\nthrow InvalidSplitException if it's off"))
y += 60
cells2.append(msg(xs["strategy: SplitStrategy"], xs["service: SplitwiseService"], y, "return splits: List<Split>", kind="return"))
y += 50
cells2.append(selfcall(xs["service: SplitwiseService"], y, "«create» new Expense(...)", loop_w=90, loop_h=22))
y += 60
cells2.append(frame(xs["service: SplitwiseService"] - 40, xs["balanceSheet: BalanceSheetRepository"] - xs["service: SplitwiseService"] + 160, y, 90,
                     "loop  [for each split]"))
y += 34
cells2.append(msg(xs["service: SplitwiseService"], xs["balanceSheet: BalanceSheetRepository"], y, "adjust(paidBy, split.user, split.amount)"))
y += 70
cells2.append(frame(xs["service: SplitwiseService"] - 40, xs["notifier: ConsoleNotifier"] - xs["service: SplitwiseService"] + 160, y, 60,
                     "loop  [for each observer]"))
y += 34
cells2.append(msg(xs["service: SplitwiseService"], xs["notifier: ConsoleNotifier"], y, "onExpenseAdded(expense)"))
y += 70
cells2.append(msg(xs["service: SplitwiseService"], xs[":Main"], y, "return expense", kind="return"))

PAGE2 = "\n".join(cells2)

# ===========================================================================
# PAGE 3: SEQUENCE — SETTLE UP
# ===========================================================================
cells3 = []
xs = {}
for name, x in [(":Main", 100), ("service: SplitwiseService", 420), ("balanceSheet: BalanceSheetRepository", 780),
                 ("notifier: ConsoleNotifier", 1140)]:
    box, xx = lifeline(x, name, bottom=460)
    cells3 += box
    xs[name] = xx

y = 120
cells3.append(msg(xs[":Main"], xs["service: SplitwiseService"], y, "settleUp(payerUserId, payeeUserId, amount)"))
y += 50
cells3.append(selfcall(xs["service: SplitwiseService"], y, "userRepository.findById(payerId), findById(payeeId)", loop_w=110, loop_h=24))
y += 60
cells3.append(msg(xs["service: SplitwiseService"], xs["balanceSheet: BalanceSheetRepository"], y, "adjust(payee, payer, -amount)"))
y += 34
cells3.append(note(xs["balanceSheet: BalanceSheetRepository"] + 20, y, 260,
                    "reduces what payer owes payee;\nboth balances[payee][payer] and\nbalances[payer][payee] update together"))
y += 60
cells3.append(msg(xs["service: SplitwiseService"], xs["notifier: ConsoleNotifier"], y, "onSettlement(payer, payee, amount)"))
y += 50
cells3.append(msg(xs["service: SplitwiseService"], xs[":Main"], y, "return", kind="return"))

PAGE3 = "\n".join(cells3)

# ===========================================================================
outpath = os.path.join(os.path.dirname(__file__), "splitwise.drawio")
write_mxfile([
    page("classDiagram", "1 - Class Diagram", PAGE1, w=2000, h=1900),
    page("seqAddExpense", "2 - Sequence - Add Expense", PAGE2, w=1900, h=820),
    page("seqSettleUp", "3 - Sequence - Settle Up", PAGE3, w=1500, h=520),
], outpath)
validate(outpath)
print("wrote", outpath)
