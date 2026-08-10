# ATM

> **Mode: Learning** — this design (happy flow, class breakdown, pattern
> choices) was authored by Claude, not the user. See `CLAUDE.md` → "Mode:
> Learning" for what that implies about ownership of the decisions below.

A single-machine ATM: a customer inserts a card, authenticates with a PIN,
and can check balance, withdraw, or deposit — where a withdrawal has to be
physically dispensed as real banknotes from whatever denominations the
machine currently has stocked.

## Happy flow

1. An admin registers accounts (`AtmService.registerAccount()`) and loads
   cash into the machine per denomination (`loadCash()`).
2. A customer inserts their card (`insertCard()`), moving the machine from
   `IDLE` to `CARD_INSERTED`.
3. They enter a PIN (`enterPin()`). A correct PIN authenticates the session
   (`AUTHENTICATED`); a wrong one stays `CARD_INSERTED` and counts against a
   3-attempt limit — the 3rd wrong PIN retains the card (`CARD_RETAINED`,
   terminal until an admin `resetMachine()`).
4. While authenticated: `checkBalance()` is a plain read; `withdraw()` and
   `deposit()` are each run as a `TransactionCommand` that validates,
   mutates the account, and returns a `TransactionReceipt` logged onto the
   account's own history (`getMiniStatement()`).
5. A withdrawal additionally has to clear the cash dispenser: the requested
   amount is planned against a chain of per-denomination handlers (largest
   note first) before any inventory is touched.
6. `ejectCard()` returns the machine to `IDLE` for the next customer.

## Design patterns used

- **State** — `state/AtmState.java` (interface with throwing defaults) plus
  `IdleState`/`CardInsertedState`/`AuthenticatedState`/`CardRetainedState`
  singletons, held directly on `AtmService` (there's one physical machine,
  same shape as chess's `ChessGameService` holding its `GameState` rather
  than a model object owning it). Each state overrides only the
  guards/transitions it allows — e.g. only `CardInsertedState` overrides
  `retainCard()`, so calling it from any other state falls through to the
  interface default and throws `IllegalAtmOperationException`.
  `AtmService` never branches on `AtmStatus` itself; it calls a guard
  (`requireAuthenticated()`, etc.) or a transition and lets the current
  state object decide.
- **Command** — `command/TransactionCommand.java` with `WithdrawCommand` and
  `DepositCommand`. Each encapsulates its own parameters (account, amount,
  and — for withdrawals — the `CashDispenser`) behind a single `execute()`,
  so `AtmService.withdraw()/deposit()` build the right command and log
  whatever `TransactionReceipt` comes back through the same
  `recordAndReturn()` path, without a type-specific branch. `checkBalance()`
  deliberately stays a plain getter rather than a command — it doesn't
  mutate anything or need a receipt, so wrapping it would just be ceremony.
- **Chain of Responsibility** — `chain/DenominationHandler.java`, the
  classic ATM cash-dispensing example: `CashDispenser` rebuilds a chain
  (largest denomination first) from a read-only snapshot on every
  `dispense()` call, each handler's `plan()` greedily claims as many of its
  own notes as it can and hands the remainder to the next (smaller)
  denomination, and only `CashDispenser` commits the result — and only
  after the *entire* amount is confirmed coverable end to end. That
  plan-then-commit split means a failed dispense (exact change unavailable)
  never leaves the inventory partially consumed; `test/output/output.txt`
  shows a failed `WITHDRAW 150` immediately followed by a successful
  `WITHDRAW 1900` using the same, untouched note counts.

## Structure

```
atm/
  src/
    model/       Account, TransactionReceipt, TransactionType, CashDispenser, AtmStatus
    state/       AtmState + Idle/CardInserted/Authenticated/CardRetainedState
    chain/       DenominationHandler
    command/     TransactionCommand, WithdrawCommand, DepositCommand
    repository/  AccountRepository (in-memory)
    exceptions/  AccountNotFoundException, InvalidPinException, InsufficientFundsException,
                 InsufficientCashException, IllegalAtmOperationException, InvalidAmountException
    services/    AtmService (the only public entry point)
    Main.java    Reads a command script, drives the service, writes a transcript
  test/
    input/scenario.txt   Happy path (insert -> PIN -> withdraw -> deposit -> eject) + every edge case below
    output/output.txt    Captured run transcript
  diagrams/
    generate.py    Data-only script that builds atm.drawio via docs/tooling/drawio_uml.py
    atm.drawio     Class diagram + 3 sequence diagrams (auth, withdraw, PIN lockout/reset)
```

## Running it

```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

Requires the VS Code "Extension Pack for Java" (Microsoft) for the
`.vscode/launch.json` run config to work — open the `atm/` folder itself as
the workspace root, then use the "Run Main (scenario.txt)" config.

## Known gaps (flagged, not fixed)

- No persistence — all state is in-memory and lost on process exit.
- No concurrency control — two threads driving the same session (or two
  sessions racing on `CashDispenser`) aren't guarded against; a real ATM is
  single-session by physical construction, which this design assumes but
  doesn't enforce.
- Amounts are `int` (whole currency units only, matching the note
  denominations) — no fractional/decimal currency support.
- `DenominationHandler.plan()` is a simple greedy chain (largest-first),
  not an optimal-change solver — a machine that's out of one denomination
  can fail to dispense an amount that a smarter combination could have
  covered (see the `WITHDRAW 150` case in the test scenario).
- Deposits don't feed back into `CashDispenser`'s inventory — modeling
  arbitrary deposited amounts as discrete notes would need a real
  cash-acceptor/recycler model, which is out of scope here; deposits only
  credit the account.
- `CardRetainedState` has no automatic timeout or admin workflow beyond the
  single `resetMachine()` override — a real ATM would notify the bank and
  require a technician visit, not a same-session reset.
