# -*- coding: utf-8 -*-
"""Regenerates atm.drawio from docs/tooling/drawio_uml.py.

Run from the repo root: python atm/diagrams/generate.py
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

cells.append(group_title(COL[0], y, "model — accounts & receipts"))
y += 34
box, account_id, h1 = uml_box(COL[0], y, 320, "Account",
    attrs=["- accountNumber: String", "- pin: String", "- balance: int",
           "- transactionHistory: List<TransactionReceipt>"],
    methods=["+ isPinCorrect(candidate): boolean", "+ getBalance(): int",
             "+ debit(amount)/credit(amount): void", "+ addTransaction(receipt): void",
             "+ getTransactionHistory(): List<TransactionReceipt>"])
cells += box
box, receipt_id, h2 = uml_box(COL[1], y, 340, "TransactionReceipt",
    attrs=["- type: TransactionType", "- amount: int", "- balanceAfter: int",
           "- denominationBreakdown: Map<Integer,Integer>"],
    methods=["+ getType()/getAmount()/getBalanceAfter()", "+ getDenominationBreakdown()",
              "  // null for deposits"])
cells += box
box, ttype_id, h3 = uml_box(COL[2], y, 260, "TransactionType", stereotype="enumeration",
    attrs=["WITHDRAWAL", "DEPOSIT"], header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
cells.append(edge(account_id, receipt_id, "composition", "transactionHistory  0..*",
                   exitX="0.5", exitY="0", entryX="0.3", entryY="1"))
cells.append(edge(receipt_id, ttype_id, "association", "type", exitX="1", exitY="0.15", entryX="0", entryY="0.5"))
row1_bottom = y + max(h1, h2, h3)

y = row1_bottom + 70
cells.append(group_title(COL[0], y, "model — cash inventory  +  chain — denomination dispensing (Chain of Responsibility)"))
y += 34
box, dispenser_id, h4 = uml_box(COL[0], y, 340, "CashDispenser",
    attrs=["- notesByDenomination: TreeMap<Integer,Integer>", "  // sorted largest first"],
    methods=["+ loadCash(denomination, count): void", "+ dispense(amount): Map<Integer,Integer>",
              "- buildChain(): DenominationHandler", "  // rebuilt fresh from a snapshot every call"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
box, handler_id, h5 = uml_box(COL[1], y, 340, "DenominationHandler",
    attrs=["- denomination: int", "- availableNotes: int  {final}", "- next: DenominationHandler"],
    methods=["+ linkWith(next): DenominationHandler",
              "+ plan(amountRemaining, breakdown): void",
              "  // greedy on this denomination, then next.plan(remainder, …)",
              "  // never mutates -- CashDispenser commits only on full success"])
cells += box
cells.append(edge(dispenser_id, handler_id, "dependency", "«builds fresh each call»", exitX="1", exitY="0.3", entryX="0", entryY="0.3"))
cells.append(edge(handler_id, handler_id, "association", "next", exitX="1", exitY="0.7", entryX="1", entryY="0.9"))
row2_bottom = y + max(h4, h5)

y = row2_bottom + 70
cells.append(group_title(COL[0], y, "state — ATM session lifecycle (State pattern, held on AtmService)"))
y += 34
box, atmstate_id, hs0 = uml_box(COL[0], y, 340, "AtmState", stereotype="interface",
    methods=["+ getStatus(): AtmStatus", "+ requireIdle()/requireCardInserted()/requireAuthenticated(): void",
              "+ insertCard()/authenticate()/ejectCard()/retainCard(): AtmState",
              "  // defaults throw IllegalAtmOperationException;", "  // each state overrides only its legal guards/moves"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, idle_id, hs1 = uml_box(COL[1], y, 280, "IdleState", stereotype="singleton",
    methods=["+ requireIdle(): void  // no-op", "+ insertCard(): AtmState"])
cells += box
box, cardins_id, hs2 = uml_box(COL[2], y, 280, "CardInsertedState", stereotype="singleton",
    methods=["+ requireCardInserted(): void  // no-op", "+ authenticate()/ejectCard()/retainCard(): AtmState"])
cells += box
box, auth_id, hs3 = uml_box(COL[3], y, 280, "AuthenticatedState", stereotype="singleton",
    methods=["+ requireAuthenticated(): void  // no-op", "+ ejectCard(): AtmState"])
cells += box
box, retained_id, hs4 = uml_box(COL[4], y, 280, "CardRetainedState", stereotype="singleton",
    methods=["  // terminal: all guards/moves throw;", "  // only AtmService.resetMachine() recovers"])
cells += box
box, astatus_id, hs5 = uml_box(COL[0], y + max(hs0, hs1, hs2, hs3, hs4) + 20, 300, "AtmStatus", stereotype="enumeration",
    attrs=["IDLE", "CARD_INSERTED", "AUTHENTICATED", "CARD_RETAINED"],
    header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
cells.append(edge(idle_id, atmstate_id, "realize", exitX="0.5", exitY="0", entryX="0.2", entryY="1"))
cells.append(edge(cardins_id, atmstate_id, "realize", exitX="0.5", exitY="0", entryX="0.45", entryY="1"))
cells.append(edge(auth_id, atmstate_id, "realize", exitX="0.5", exitY="0", entryX="0.7", entryY="1"))
cells.append(edge(retained_id, atmstate_id, "realize", exitX="0.5", exitY="0", entryX="1", entryY="0.7"))
cells.append(edge(idle_id, cardins_id, "dependency", "insertCard() →", exitX="1", exitY="0.3", entryX="0", entryY="0.3"))
cells.append(edge(cardins_id, auth_id, "dependency", "authenticate() →", exitX="1", exitY="0.3", entryX="0", entryY="0.3"))
cells.append(edge(cardins_id, retained_id, "dependency", "retainCard() → (3 bad PINs)", exitX="1", exitY="0.6", entryX="0", entryY="0.6"))
cells.append(edge(atmstate_id, astatus_id, "association", "getStatus()", exitX="0.2", exitY="1", entryX="0.5", entryY="0"))
row3_bottom = y + max(hs0, hs1, hs2, hs3, hs4) + 20 + hs5

y = row3_bottom + 70
cells.append(group_title(COL[0], y, "command — transactions (Command pattern)"))
y += 34
box, cmd_id, hc0 = uml_box(COL[0], y, 320, "TransactionCommand", stereotype="interface",
    methods=["+ execute(): TransactionReceipt"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, withdraw_id, hc1 = uml_box(COL[1], y, 320, "WithdrawCommand",
    attrs=["- account: Account", "- amount: int", "- cashDispenser: CashDispenser"],
    methods=["+ execute(): TransactionReceipt", "  // funds check, then dispenser.dispense(), then debit"])
cells += box
box, deposit_id, hc2 = uml_box(COL[2], y, 300, "DepositCommand",
    attrs=["- account: Account", "- amount: int"],
    methods=["+ execute(): TransactionReceipt", "  // credit only, no dispenser involved"])
cells += box
cells.append(edge(withdraw_id, cmd_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
cells.append(edge(deposit_id, cmd_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.6"))
cells.append(edge(withdraw_id, dispenser_id, "dependency", "uses", exitX="0.5", exitY="0", entryX="0.7", entryY="1"))
cells.append(edge(withdraw_id, account_id, "dependency", "debits", exitX="0", exitY="0.3", entryX="1", entryY="0.6"))
row4_bottom = y + max(hc0, hc1, hc2)

y = row4_bottom + 70
cells.append(group_title(COL[0], y, "repository (in-memory)  +  exceptions"))
y += 34
box, acctrepo_id, hr1 = uml_box(COL[0], y, 320, "AccountRepository",
    attrs=["- accountsByNumber: Map<String,Account>"],
    methods=["+ save(account): void", "+ findByAccountNumber(number): Account",
              "  // throws AccountNotFoundException"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
cells.append(edge(acctrepo_id, account_id, "aggregation", "0..*", exitX="1", exitY="0.5", entryX="0", entryY="0.7"))

box, rte_id, he0 = uml_box(COL[1], y, 260, "RuntimeException", stereotype="java.lang", dashed=True,
    header_fill="#f5f5f5", header_stroke="#666666")
cells += box
box, anf_id, he1 = uml_box(COL[2], y, 280, "AccountNotFoundException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, ipe_id, he2 = uml_box(COL[3], y, 280, "InvalidPinException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
row5a_bottom = y + max(hr1, he0, he1, he2)
y = row5a_bottom + 20
box, iff_id, he3 = uml_box(COL[1], y, 280, "InsufficientFundsException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, icf_id, he4 = uml_box(COL[2], y, 280, "InsufficientCashException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, iao_id, he5 = uml_box(COL[3], y, 280, "IllegalAtmOperationException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
row5b_bottom = y + max(he3, he4, he5)
y = row5b_bottom + 20
box, iva_id, he6 = uml_box(COL[1], y, 280, "InvalidAmountException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
for eid in (anf_id, ipe_id):
    cells.append(edge(eid, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
for eid in (iff_id, icf_id, iao_id):
    cells.append(edge(eid, rte_id, "inherit", exitX="0", exitY="0", entryX="1", entryY="0.6"))
cells.append(edge(iva_id, rte_id, "inherit", exitX="0", exitY="0", entryX="1", entryY="0.9"))
row5_bottom = y + he6

y = row5_bottom + 80
cells.append(group_title(COL[0], y, "services — the ONE public entry point"))
y += 34
box, svc_id, hsv = uml_box(COL[0], y, 560, "AtmService",
    attrs=["- MAX_PIN_ATTEMPTS: int {static final}", "- accountRepository: AccountRepository",
           "- cashDispenser: CashDispenser", "- currentState: AtmState", "- currentAccountNumber: String",
           "- pinAttempts: int"],
    methods=["+ registerAccount(number, pin, balance): void", "+ loadCash(denomination, count): void",
              "+ insertCard(number)/enterPin(pin): void", "+ checkBalance(): int",
              "+ withdraw(amount)/deposit(amount): TransactionReceipt",
              "+ getMiniStatement(): List<TransactionReceipt>",
              "+ ejectCard(): void", "+ resetMachine(): void  // admin override",
              "+ getStatus(): AtmStatus",
              "- recordAndReturn(command): TransactionReceipt", "- currentAccount(): Account"],
    header_fill="#dae8fc", header_stroke="#6c8ebf")
cells += box

cells.append(edge(svc_id, acctrepo_id, "composition", "accountRepository  1", exitX="0.1", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, dispenser_id, "composition", "cashDispenser  1", exitX="0.3", exitY="0", entryX="0.7", entryY="1"))
cells.append(edge(svc_id, atmstate_id, "association", "currentState  1", exitX="0.55", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, cmd_id, "dependency", "creates & executes", exitX="0.8", exitY="0", entryX="0.5", entryY="1"))

PAGE1 = "\n".join(cells)

# ===========================================================================
# PAGE 2: SEQUENCE — INSERT CARD + PIN (correct on first try)
# ===========================================================================
cells2 = []
xs = {}
for name, x in [(":Main", 120), ("svc: AtmService", 420), ("state: AtmState", 740),
                 ("acctRepo: AccountRepository", 1040), ("account: Account", 1340)]:
    box, xx = lifeline(x, name, bottom=760)
    cells2 += box
    xs[name] = xx

y = 120
cells2.append(msg(xs[":Main"], xs["svc: AtmService"], y, "insertCard(accountNumber)"))
y += 40
cells2.append(msg(xs["svc: AtmService"], xs["state: AtmState"], y, "requireIdle()   // current state = IdleState.INSTANCE"))
y += 44
cells2.append(msg(xs["svc: AtmService"], xs["acctRepo: AccountRepository"], y, "findByAccountNumber(accountNumber)   // validates existence"))
y += 44
cells2.append(msg(xs["svc: AtmService"], xs["state: AtmState"], y, "state = state.insertCard()"))
y += 40
cells2.append(msg(xs["state: AtmState"], xs["svc: AtmService"], y, "return CardInsertedState.INSTANCE", kind="return"))
y += 60
cells2.append(msg(xs[":Main"], xs["svc: AtmService"], y, "enterPin(pin)"))
y += 40
cells2.append(msg(xs["svc: AtmService"], xs["state: AtmState"], y, "requireCardInserted()   // current state = CardInsertedState.INSTANCE"))
y += 44
cells2.append(msg(xs["svc: AtmService"], xs["acctRepo: AccountRepository"], y, "findByAccountNumber(currentAccountNumber)"))
y += 40
cells2.append(msg(xs["acctRepo: AccountRepository"], xs["svc: AtmService"], y, "return account", kind="return"))
y += 44
cells2.append(msg(xs["svc: AtmService"], xs["account: Account"], y, "isPinCorrect(pin)"))
y += 40
cells2.append(frame(xs["svc: AtmService"] - 60, xs["account: Account"] - xs["svc: AtmService"] + 160, y, 100,
                     "alt  [pin correct]"))
cells2.append(msg(xs["svc: AtmService"], xs["state: AtmState"], y + 30, "state = state.authenticate()"))
cells2.append(divider(xs["svc: AtmService"] - 60, xs["account: Account"] - xs["svc: AtmService"] + 160, y + 65, "[else: wrong PIN, see page 4 for the lockout path]"))
y += 120
cells2.append(msg(xs["svc: AtmService"], xs[":Main"], y, "return  (state = AuthenticatedState.INSTANCE)", kind="return"))

PAGE2 = "\n".join(cells2)

# ===========================================================================
# PAGE 3: SEQUENCE — WITHDRAW (Command + Chain of Responsibility)
# ===========================================================================
cells3 = []
xs = {}
for name, x in [(":Main", 100), ("svc: AtmService", 380), ("cmd: WithdrawCommand", 680),
                 ("dispenser: CashDispenser", 980), ("handler: DenominationHandler", 1280), ("account: Account", 1560)]:
    box, xx = lifeline(x, name, bottom=1060)
    cells3 += box
    xs[name] = xx

y = 120
cells3.append(msg(xs[":Main"], xs["svc: AtmService"], y, "withdraw(amount)"))
y += 40
cells3.append(msg(xs["svc: AtmService"], xs["cmd: WithdrawCommand"], y, "«create» new WithdrawCommand(account, amount, cashDispenser)", kind="create"))
y += 44
cells3.append(msg(xs["svc: AtmService"], xs["cmd: WithdrawCommand"], y, "execute()"))
y += 40
cells3.append(frame(xs["cmd: WithdrawCommand"] - 60, xs["account: Account"] - xs["cmd: WithdrawCommand"] + 160, y, 90,
                     "alt  [amount > account.getBalance()]"))
cells3.append(selfcall(xs["cmd: WithdrawCommand"], y + 25, "throw InsufficientFundsException", loop_w=90, loop_h=20))
cells3.append(divider(xs["cmd: WithdrawCommand"] - 60, xs["account: Account"] - xs["cmd: WithdrawCommand"] + 160, y + 55, "[else: funds ok]"))
y += 100
cells3.append(msg(xs["cmd: WithdrawCommand"], xs["dispenser: CashDispenser"], y, "dispense(amount)"))
y += 40
cells3.append(selfcall(xs["dispenser: CashDispenser"], y, "buildChain()   // fresh snapshot, largest denomination first", loop_w=110, loop_h=22))
y += 50
cells3.append(msg(xs["dispenser: CashDispenser"], xs["handler: DenominationHandler"], y, "plan(amount, breakdown)"))
y += 40
cells3.append(frame(xs["dispenser: CashDispenser"] - 40, xs["handler: DenominationHandler"] - xs["dispenser: CashDispenser"] + 140, y, 130,
                     "loop  [recurse to next handler while remaining > 0]"))
y += 34
cells3.append(selfcall(xs["handler: DenominationHandler"], y, "notesUsable = min(remaining/denom, availableNotes)", loop_w=130, loop_h=22))
y += 50
cells3.append(msg(xs["handler: DenominationHandler"], xs["handler: DenominationHandler"], y, "next.plan(remainder, breakdown)   // self-loop: next link in chain", kind="call"))
y += 60
cells3.append(note(xs["handler: DenominationHandler"] + 40, y, 300,
                    "if remaining > 0 and next == null:\nthrow InsufficientCashException\n(no inventory touched yet)"))
y += 90
cells3.append(msg(xs["handler: DenominationHandler"], xs["dispenser: CashDispenser"], y, "return  (breakdown fully planned)", kind="return"))
y += 50
cells3.append(selfcall(xs["dispenser: CashDispenser"], y, "commit: notesByDenomination -= breakdown", loop_w=120, loop_h=22))
y += 50
cells3.append(msg(xs["dispenser: CashDispenser"], xs["cmd: WithdrawCommand"], y, "return breakdown", kind="return"))
y += 50
cells3.append(msg(xs["cmd: WithdrawCommand"], xs["account: Account"], y, "debit(amount)"))
y += 44
cells3.append(msg(xs["cmd: WithdrawCommand"], xs["svc: AtmService"], y, "return new TransactionReceipt(WITHDRAWAL, amount, balance, breakdown)", kind="return"))
y += 50
cells3.append(msg(xs["svc: AtmService"], xs["account: Account"], y, "addTransaction(receipt)   // recordAndReturn()"))
y += 44
cells3.append(msg(xs["svc: AtmService"], xs[":Main"], y, "return receipt", kind="return"))

PAGE3 = "\n".join(cells3)

# ===========================================================================
# PAGE 4: SEQUENCE — PIN LOCKOUT + ADMIN RESET (State pattern edge case)
# ===========================================================================
cells4 = []
xs = {}
for name, x in [(":Main", 120), ("svc: AtmService", 420), ("state: AtmState", 740)]:
    box, xx = lifeline(x, name, bottom=760)
    cells4 += box
    xs[name] = xx

y = 120
cells4.append(note(xs[":Main"] - 60, y, 320, "3rd consecutive wrong PIN for this card\n(pinAttempts already at 2 from prior calls)"))
y += 70
cells4.append(msg(xs[":Main"], xs["svc: AtmService"], y, "enterPin(wrongPin)"))
y += 40
cells4.append(msg(xs["svc: AtmService"], xs["state: AtmState"], y, "requireCardInserted()"))
y += 44
cells4.append(selfcall(xs["svc: AtmService"], y, "account.isPinCorrect(wrongPin) -> false; pinAttempts++ (now 3)", loop_w=140, loop_h=22))
y += 60
cells4.append(frame(xs["svc: AtmService"] - 60, xs["state: AtmState"] - xs["svc: AtmService"] + 160, y, 100,
                     "alt  [pinAttempts >= MAX_PIN_ATTEMPTS]"))
cells4.append(msg(xs["svc: AtmService"], xs["state: AtmState"], y + 30, "state = state.retainCard()"))
cells4.append(divider(xs["svc: AtmService"] - 60, xs["state: AtmState"] - xs["svc: AtmService"] + 160, y + 68, "[else: throw InvalidPinException with attempt count]"))
y += 120
cells4.append(msg(xs["state: AtmState"], xs["svc: AtmService"], y, "return CardRetainedState.INSTANCE", kind="return"))
y += 40
cells4.append(selfcall(xs["svc: AtmService"], y, "throw InvalidPinException(\"card retained\")", loop_w=110, loop_h=22))
y += 60
cells4.append(msg(xs[":Main"], xs["svc: AtmService"], y, "insertCard(otherAccountNumber)   // next command in the script"))
y += 40
cells4.append(msg(xs["svc: AtmService"], xs["state: AtmState"], y, "requireIdle()   // current state = CardRetainedState.INSTANCE"))
y += 44
cells4.append(selfcall(xs["state: AtmState"], y, "throw IllegalAtmOperationException   // interface default; CardRetainedState overrides nothing", loop_w=140, loop_h=22))
y += 70
cells4.append(msg(xs[":Main"], xs["svc: AtmService"], y, "resetMachine()   // administrative override, not a state transition"))
y += 44
cells4.append(selfcall(xs["svc: AtmService"], y, "currentState = IdleState.INSTANCE  (direct assignment)", loop_w=140, loop_h=22))
y += 50
cells4.append(msg(xs["svc: AtmService"], xs[":Main"], y, "return", kind="return"))

PAGE4 = "\n".join(cells4)

# ===========================================================================
outpath = os.path.join(os.path.dirname(__file__), "atm.drawio")
write_mxfile([
    page("classDiagram", "1 - Class Diagram", PAGE1, w=2000, h=2500),
    page("seqAuth", "2 - Sequence - Insert Card + PIN", PAGE2, w=1700, h=850),
    page("seqWithdraw", "3 - Sequence - Withdraw (Command + CoR)", PAGE3, w=1900, h=1100),
    page("seqLockout", "4 - Sequence - PIN Lockout + Reset", PAGE4, w=1700, h=800),
], outpath)
validate(outpath)
print("wrote", outpath)
