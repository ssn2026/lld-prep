# ATM — Simple Walkthrough

This file explains the whole codebase in plain language, in the order you
should read it. No prior context assumed.

---

## 1. The story in one paragraph

You're modeling a single physical ATM machine. A customer walks up, inserts
their card, types a PIN, and — if it's correct — can check their balance,
withdraw cash, or deposit money. A withdrawal is the interesting part: the
machine doesn't just subtract a number from a balance, it also has to
physically hand over real banknotes, and it only has a limited stock of each
denomination (2000s, 500s, 200s, 100s, etc.) sitting in its cash tray. So the
system has two things to get right at once: "is this customer allowed to do
this right now?" (you can't withdraw before you've logged in, you can't
insert a second card while one is already in the slot) and "can the machine
actually make correct change with the notes it currently has?".

---

## 2. The one door you're allowed to knock on

`src/services/AtmService.java` is the **only** class anything outside the
package is meant to call. Everything else (`model`, `state`, `command`,
`chain`, `repository`, `exceptions`) is a helper `AtmService` uses
internally.

| Method | What it does |
|---|---|
| `registerAccount(accountNumber, pin, initialBalance)` | Admin operation: create an account |
| `loadCash(denomination, count)` | Admin operation: stock the machine with notes |
| `insertCard(accountNumber)` | Start a session — machine goes `IDLE` -> `CARD_INSERTED` |
| `enterPin(pin)` | Authenticate the session — `CARD_INSERTED` -> `AUTHENTICATED` (or a strike against the 3-attempt limit) |
| `checkBalance()` | Plain read of the current account's balance |
| `withdraw(amount)` | Validate, dispense physical notes, debit the account, return a `TransactionReceipt` |
| `deposit(amount)` | Validate, credit the account, return a `TransactionReceipt` |
| `getMiniStatement()` | Return the current account's transaction history |
| `ejectCard()` | End the session — back to `IDLE` |
| `resetMachine()` | Admin override — force the machine back to `IDLE` from any state, e.g. after a card retention |
| `getStatus()` | What state the machine is currently in (`AtmStatus`) |

---

## 3. Read the code in this order

### Step 1 — the building blocks (`src/model/`)

These are mostly just data, with a couple of small but meaningful behaviors
attached.

- **`Account.java`** — account number, PIN, balance, and a growing list of
  `TransactionReceipt`s. `isPinCorrect(candidatePin)` does the actual PIN
  check (a simple string comparison) so `AtmService` never touches the raw
  `pin` field directly. `debit`/`credit` mutate the balance;
  `addTransaction` appends to the history.
- **`AtmStatus.java`** — an enum: `IDLE`, `CARD_INSERTED`, `AUTHENTICATED`,
  `CARD_RETAINED`. This is just the label for "which state is the machine
  in" — the actual *behavior* attached to each status lives in `state/`
  (next section), not here.
- **`TransactionReceipt.java`** — what a completed withdrawal or deposit
  produced: the type, the amount, the resulting balance, and (for
  withdrawals only) a `Map<Integer, Integer>` breakdown of which
  denominations were handed out, e.g. `{2000: 1, 500: 3}`. It's explicitly
  documented as "null for deposits" since a deposit never touches the note
  breakdown.
- **`TransactionType.java`** — enum: `WITHDRAWAL`, `DEPOSIT`.
- **`CashDispenser.java`** — this one is more than plain data; it owns the
  machine's physical cash inventory (`notesByDenomination`, a `TreeMap`
  sorted largest-denomination-first) and is where the Chain of
  Responsibility pattern actually runs from. Covered in Step 3 below, after
  you've seen the chain classes it drives.

### Step 2 — is this customer allowed to do that right now? (`src/state/`)

This is the **State** pattern: instead of `AtmService` holding an
`AtmStatus` enum and writing `if (status == AUTHENTICATED) { ... } else if
...` everywhere an operation needs to check "are we allowed to do this?",
each state is its own small object that knows what's legal *from itself*.

- **`AtmState.java`** — an interface with one real method
  (`getStatus()`) and a set of **guard methods**
  (`requireIdle()`, `requireCardInserted()`, `requireAuthenticated()`) and
  **transition methods** (`insertCard()`, `authenticate()`, `ejectCard()`,
  `retainCard()`). Every one of the guard/transition methods has a
  `default` implementation right on the interface that just throws
  `IllegalAtmOperationException`. That default-throws trick is the whole
  point of this design: a concrete state only needs to override the handful
  of methods it actually allows, and everything it doesn't override
  automatically falls through to "not allowed from here."
- **`IdleState.java`** — overrides `requireIdle()` (does nothing, i.e.
  succeeds) and `insertCard()` (returns `CardInsertedState.INSTANCE`).
  Nothing else — you can't authenticate, check balance, or eject a card
  from `IDLE`, so those all fall through to the interface's throwing
  defaults.
- **`CardInsertedState.java`** — overrides `requireCardInserted()`,
  `authenticate()` (-> `AuthenticatedState.INSTANCE`), `ejectCard()` (->
  `IdleState.INSTANCE`, e.g. the customer gives up before entering a PIN),
  and `retainCard()` (-> `CardRetainedState.INSTANCE`, the 3-wrong-PINs
  case).
- **`AuthenticatedState.java`** — overrides `requireAuthenticated()` and
  `ejectCard()` (-> `IdleState.INSTANCE`).
- **`CardRetainedState.java`** — overrides **nothing** except `getStatus()`.
  Every guard and every transition falls through to the throwing default.
  This is a deliberate dead end: once the machine has swallowed a card
  after 3 wrong PINs, nothing short of an administrator can recover it
  (`AtmService.resetMachine()`, which isn't a state transition at all — it
  just force-assigns `IdleState.INSTANCE` directly, bypassing the state
  machine entirely, the same way a technician would physically intervene).

Each of the four states is a `public static final` singleton (`INSTANCE`)
with a private constructor — there's only ever one physical machine, so
there's no reason to allocate a new `IdleState` object every time the
machine goes idle. `AtmService` holds a single `currentState` field and
never branches on `AtmStatus` itself: it just calls a guard (e.g.
`currentState.requireAuthenticated()`) or a transition (e.g. `currentState =
currentState.insertCard()`) and lets whichever state object is currently
active decide what happens.

### Step 3 — how does a withdrawal turn into physical notes? (`src/chain/` and `CashDispenser`)

This is the **Chain of Responsibility** pattern, and it's the classic
"ATM cash dispensing" textbook example.

- **`DenominationHandler.java`** — one handler per denomination (e.g. one
  for 2000-notes, one for 500-notes, etc.), each holding how many notes of
  that denomination are available and a `next` pointer to the handler for
  the next-smaller denomination. `plan(amountRemaining, breakdown)` greedily
  takes as many of its own notes as it can (`Math.min(amountRemaining /
  denomination, availableNotes)`), records that count into the shared
  `breakdown` map if it used any, and — if there's still an amount left
  over — hands the *remainder* off to `next.plan(...)`. If there's a
  remainder but no `next` handler left (you've run out of denominations),
  it throws `InsufficientCashException`. Critically, `plan()` never
  mutates the machine's real inventory — it only reads `availableNotes`
  and writes into the caller-supplied `breakdown` map.
- **`CashDispenser.dispense(amount)`** — this is the one that actually owns
  the real inventory (`notesByDenomination`) and is the only place that
  chain gets built and committed. On every single call it:
  1. Rebuilds the entire chain from scratch, largest-denomination-first
     (`buildChain()`, which walks the `TreeMap` — already sorted
     descending — and links each `DenominationHandler` to the next).
  2. Calls `chainHead.plan(amount, breakdown)` against that fresh,
     read-only snapshot.
  3. Only *after* `plan()` returns successfully (meaning the full amount
     was fully covered end to end) does it actually debit
     `notesByDenomination` for every denomination in the resulting
     breakdown.

  The reason it's built this way — plan first, commit second — is so that
  a failed dispense (the machine genuinely can't make exact change) never
  leaves the inventory half-consumed. If step 2 throws partway through the
  chain, step 3 never runs at all, so a later retry sees the exact same
  note counts as before the failed attempt. You can see this proven for
  real in the captured test output (§5 below): a failed `WITHDRAW 150` is
  immediately followed by a successful `WITHDRAW 1900` using notes that
  were never touched by the failed attempt.

### Step 4 — turning a withdrawal/deposit into one uniform call (`src/command/`)

This is the **Command** pattern: instead of `AtmService.withdraw()` and
`AtmService.deposit()` each independently validating, mutating, and
building a receipt with their own separate code paths, both are wrapped as
objects that expose one method, `execute()`, and `AtmService` runs either
one through the exact same follow-up logic.

- **`TransactionCommand.java`** — the interface: just `TransactionReceipt
  execute()`.
- **`WithdrawCommand.java`** — constructed with the `Account`, the amount,
  and the `CashDispenser`. `execute()` checks the amount is positive,
  checks the account has enough balance, calls
  `cashDispenser.dispense(amount)` (which is where the chain-of-
  responsibility logic from Step 3 actually runs), debits the account, and
  returns a `TransactionReceipt` carrying the denomination breakdown.
- **`DepositCommand.java`** — constructed with just the `Account` and the
  amount. `execute()` checks the amount is positive, credits the account,
  and returns a `TransactionReceipt` with a `null` breakdown (deposits
  never touch the cash dispenser — see the README's "Known gaps": modeling
  arbitrary deposited cash as discrete notes going back into inventory was
  deliberately left out of scope).

Notice `checkBalance()` on `AtmService` is deliberately **not** wrapped as a
command — it doesn't mutate anything and produces no receipt, so wrapping it
would just be ceremony for no benefit; it's a plain getter instead.

### Step 5 — looking accounts up (`src/repository/AccountRepository.java`)

A thin wrapper around a `Map<String, Account>` (a `LinkedHashMap`, so
accounts stay in insertion order if you ever iterate them, though nothing
currently does). Two operations: `save(account)` and
`findByAccountNumber(accountNumber)`, the latter throwing
`AccountNotFoundException` if the number isn't registered. This exists so
account lookup by number is a single map access rather than scanning a
list, and so "what if the account doesn't exist" is handled in exactly one
place instead of being re-checked everywhere an account is looked up.

### Step 6 — errors (`src/exceptions/`)

All six are small `RuntimeException` subclasses that take a message string
and do nothing else — the point of having six distinct *classes* rather
than one generic exception with an error code is that `Main.java`'s
`catch` clause and any future caller can tell failure modes apart by Java
type, not by parsing a string:

- `AccountNotFoundException` — unknown account number.
- `InvalidPinException` — wrong PIN entered (also carries the "3rd attempt,
  card retained" message).
- `InsufficientFundsException` — withdrawal amount exceeds the account
  balance.
- `InsufficientCashException` — the machine physically can't make exact
  change (chain of responsibility ran out of denominations), or has no
  cash loaded at all.
- `IllegalAtmOperationException` — a State-pattern guard/transition was
  rejected (wrong machine state for the requested operation).
- `InvalidAmountException` — a withdrawal or deposit amount that's zero or
  negative.

### Step 7 — the orchestrator (`src/services/AtmService.java`)

Now that you've seen every piece, this file just wires them together. A
few details worth calling out by name:

- It holds exactly one `AtmState currentState` field (starting at
  `IdleState.INSTANCE`), one `CashDispenser`, one `AccountRepository`, and
  session-scoped fields `currentAccountNumber` / `pinAttempts` that only
  mean something while a card is inserted.
- `insertCard()` calls `currentState.requireIdle()` **before** even looking
  up the account — so inserting a card while the machine is mid-session (or
  card-retained) fails immediately via the state guard, without touching
  the repository at all.
- `enterPin()` is the one place that manually counts `pinAttempts` and
  decides whether to escalate to `retainCard()` — the state machine itself
  doesn't know about "3 attempts," it only knows how to transition *given*
  a decision `AtmService` already made.
- `withdraw()`/`deposit()` both funnel through a private helper,
  `recordAndReturn(TransactionCommand command)`, which calls
  `command.execute()` and then appends the resulting receipt onto the
  current account's history — this is the one place both commands'
  results get logged, so neither `WithdrawCommand` nor `DepositCommand`
  needs to know about transaction history at all.
- `resetMachine()` is explicitly commented as an *administrative override,
  not a state transition* — it assigns `IdleState.INSTANCE` directly
  instead of calling any transition method on the current state, which is
  exactly how it manages to recover the machine even from the otherwise
  inescapable `CardRetainedState`.

### Step 8 — the runner (`src/Main.java`)

Not part of the "real" system — a test harness. It reads a text file line
by line (`test/input/scenario.txt`), turns each line into a call on
`AtmService`, and writes what happened to `test/output/output.txt`. Its
small command language: `ACCOUNT`, `CASH`, `INSERT`, `PIN`, `BALANCE`,
`WITHDRAW`, `DEPOSIT`, `STATEMENT`, `EJECT`, `RESET`, `STATUS`. Any of the
six exception types thrown by `AtmService` gets caught in one `catch`
clause and turned into an `ERROR <ExceptionClassName>: <message>` line
instead of crashing the whole run.

---

## 4. Order of operations — two traces through the real code

### Trace A — insert card, wrong PIN, right PIN, withdraw, eject

```
Main.java: "INSERT ACC001"
   |
   v
AtmService.insertCard("ACC001")
   | currentState.requireIdle()          -- IdleState: succeeds (no-op)
   | accountRepository.findByAccountNumber("ACC001")   -- must exist, or throws
   | currentState = currentState.insertCard()   -- IdleState -> CardInsertedState.INSTANCE
   | currentAccountNumber = "ACC001"; pinAttempts = 0

Main.java: "PIN 9999"  (wrong)
   v
AtmService.enterPin("9999")
   | currentState.requireCardInserted()   -- CardInsertedState: succeeds
   | account.isPinCorrect("9999")         -- false
   | pinAttempts = 1  (< 3, no retention yet)
   | throws InvalidPinException("Incorrect PIN (attempt 1/3)")

Main.java: "PIN 1234"  (correct)
   v
AtmService.enterPin("1234")
   | account.isPinCorrect("1234")         -- true
   | currentState = currentState.authenticate()   -- CardInsertedState -> AuthenticatedState.INSTANCE
   | pinAttempts = 0

Main.java: "WITHDRAW 3800"
   v
AtmService.withdraw(3800)
   | currentState.requireAuthenticated()   -- AuthenticatedState: succeeds
   | recordAndReturn(new WithdrawCommand(account, 3800, cashDispenser))
   |    WithdrawCommand.execute()
   |       amount > 0 ? yes
   |       amount <= account.getBalance() (5000)? yes
   |       cashDispenser.dispense(3800)
   |          buildChain()  -- fresh handlers: 2000 -> 500 -> 200 -> 100
   |          chainHead.plan(3800, breakdown)
   |             2000-handler: takes 1x2000, remaining 1800
   |             500-handler:  takes 3x500=1500, remaining 300
   |             200-handler:  takes 1x200, remaining 100
   |             100-handler:  takes 1x100, remaining 0 -> done
   |          notesByDenomination debited by the breakdown
   |       account.debit(3800)   -- balance 5000 -> 1200
   |       returns TransactionReceipt(WITHDRAWAL, 3800, 1200, {2000:1,500:3,200:1,100:1})
   |    account.addTransaction(receipt)
   v
Main.java prints "OK withdrew 3800 -> balance=1200 dispensed={1x2000 3x500 1x200 1x100}"

Main.java: "EJECT"
   v
AtmService.ejectCard()
   | currentState = currentState.ejectCard()   -- AuthenticatedState -> IdleState.INSTANCE
   | currentAccountNumber = null; pinAttempts = 0
```

### Trace B — three wrong PINs retains the card

```
INSERT ACC002 -> CardInsertedState, pinAttempts=0
PIN 0000 (wrong) -> pinAttempts=1, throws InvalidPinException("...attempt 1/3")
PIN 1111 (wrong) -> pinAttempts=2, throws InvalidPinException("...attempt 2/3")
PIN 2222 (wrong) -> pinAttempts=3
   | pinAttempts >= MAX_PIN_ATTEMPTS (3)
   | currentState = currentState.retainCard()   -- CardInsertedState -> CardRetainedState.INSTANCE
   | throws InvalidPinException("Incorrect PIN entered 3 times; card retained")

STATUS -> CARD_RETAINED

INSERT ACC001
   | currentState.requireIdle()   -- CardRetainedState overrides nothing,
   |    falls through to the interface default -> throws IllegalAtmOperationException

RESET
   | AtmService.resetMachine()  -- bypasses the state machine entirely,
   |    directly assigns currentState = IdleState.INSTANCE

STATUS -> IDLE   -- machine is usable again
```

---

## 5. Reading the actual captured run (`test/output/output.txt`)

A few real lines from the run, annotated.

The wrong-PIN-then-right-PIN sequence:

```
> PIN 9999
ERROR InvalidPinException: Incorrect PIN (attempt 1/3)
> PIN 1234
OK PIN accepted, session authenticated
```

This confirms `pinAttempts` is per-session and counts up but doesn't lock
until it hits 3 — one wrong guess is just a warning, and a subsequent
correct PIN resets `pinAttempts` back to 0 in `enterPin()`.

The withdrawal that exercises the Chain of Responsibility:

```
> WITHDRAW 3800
OK withdrew 3800 -> balance=1200 dispensed={1x2000 3x500 1x200 1x100}
```

1x2000 + 3x500 (1500) + 1x200 + 1x100 = 2000+1500+200+100 = 3800 — the
breakdown really does sum to the requested amount, largest denomination
first, exactly as `DenominationHandler.plan()`'s greedy walk would produce
given the loaded stock (`CASH 2000 5`, `CASH 500 10`, `CASH 200 10`, `CASH
100 20` from the top of `scenario.txt`).

The plan-then-commit guarantee, proven end to end:

```
> WITHDRAW 100000
ERROR InsufficientFundsException: Insufficient balance: requested 100000, available 2000
> WITHDRAW 150
ERROR InsufficientCashException: Cannot dispense exact amount; no smaller denomination available for the remaining 50
> WITHDRAW 1900
OK withdrew 1900 -> balance=100 dispensed={3x500 2x200}
```

At this point in the script, ACC002's remaining cash-relevant denominations
are down to 500s and 200s (the 100-notes were exhausted by the earlier
withdrawal in the ACC001 session). `WITHDRAW 150` fails because after using
zero 500s and zero 200s (150 is smaller than both), there's no 100- or
smaller-denomination handler left in the chain to cover the remaining 50 —
`InsufficientCashException` is thrown *before* `CashDispenser.dispense()`
ever debits `notesByDenomination`. The very next line, `WITHDRAW 1900`,
succeeds and dispenses `{3x500 2x200}` (1500 + 400 = 1900) — proof that the
failed 150 attempt left the 500/200 inventory completely untouched, exactly
as the plan-then-commit design in `CashDispenser.dispense()` promises.

The terminal `CardRetainedState` and its only way out:

```
> PIN 2222
ERROR InvalidPinException: Incorrect PIN entered 3 times; card retained
> STATUS
STATUS -> CARD_RETAINED
> INSERT ACC001
ERROR IllegalAtmOperationException: Requires an idle machine; currently CARD_RETAINED
> RESET
OK machine reset to idle
> STATUS
STATUS -> IDLE
```

Ending the file, two calls made on an idle, card-less machine:

```
> BALANCE
ERROR IllegalAtmOperationException: Requires an authenticated session; currently IDLE
> WITHDRAW 100
ERROR IllegalAtmOperationException: Requires an authenticated session; currently IDLE
```

Both fail with the exact same exception type and reason, because both
`checkBalance()` and `withdraw()` start with the identical guard call,
`currentState.requireAuthenticated()` — one line of shared logic covering
every authenticated-only operation.

---

## 6. Things to try / test yourself

You can edit `test/input/scenario.txt` and re-run with:
```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

1. **Deposit, then check the statement.** Add a `DEPOSIT 500` followed by a
   `STATEMENT` after an authenticated session. Confirm the printed line has
   no denomination breakdown after it (unlike a `WITHDRAWAL` line) — proof
   that `DepositCommand` really does pass `null` for the breakdown.
2. **Force a `CardRetainedState` recovery without `RESET`.** Try any
   operation (`PIN`, `BALANCE`, `WITHDRAW`, `EJECT`) right after 3 wrong
   PINs, before calling `RESET`. All of them should fail with
   `IllegalAtmOperationException`, since `CardRetainedState` overrides
   nothing but `getStatus()`.
3. **Drain one denomination completely, then request an amount only
   coverable by a smarter combination.** E.g. load only `CASH 100 1` and
   `CASH 50 1` and try `WITHDRAW 150` — the greedy chain will take the
   single 100, then fail on the 50 remainder if there's no 50-denomination
   handler, even though the correct notes technically exist. This exposes
   the "greedy, not optimal" limitation called out in the README's "Known
   gaps."
4. **Withdraw or deposit a negative or zero amount.** `WITHDRAW 0` or
   `WITHDRAW -50`. Expect `InvalidAmountException` — this check happens
   inside `WithdrawCommand.execute()`, before the balance or the cash
   dispenser is even consulted.
5. **Insert a card for an unregistered account.** `INSERT GHOST999` with no
   matching `ACCOUNT` line. Expect `AccountNotFoundException`, thrown by
   `AccountRepository.findByAccountNumber` — and notice the state machine
   is untouched (still `IDLE`) since the exception happens after
   `requireIdle()` passes but before `insertCard()` transitions anything.
6. **Try to withdraw more than the balance but less than the machine's
   cash.** Confirms `InsufficientFundsException` is checked and thrown
   *before* `cashDispenser.dispense()` is ever called — trace it in
   `WithdrawCommand.execute()`, where the balance check is the second `if`,
   strictly before the `dispense()` call.
