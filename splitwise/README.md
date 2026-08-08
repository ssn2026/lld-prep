# Splitwise

> **Mode: Learning** — this design (happy flow, class breakdown, pattern
> choices) was authored by Claude, not the user. See `CLAUDE.md` → "Mode:
> Learning" for what that implies about ownership of the decisions below.

An expense-splitting system: users add shared expenses (split equally,
by exact amounts, or by percentage), the system tracks who owes whom, and
users can settle debts and query balances.

## Happy flow

1. Users are registered via `SplitwiseService.addUser()`.
2. A user adds an expense they paid for (`addExpense()`), naming the
   participants and how it should be split. The `SplitStrategy` for the
   requested `SplitType` computes each participant's share.
3. `SplitwiseService` updates the pairwise balance sheet: everyone in the
   split (other than the payer) now owes the payer their share.
4. Any registered `ExpenseObserver` is notified the expense was added.
5. Balances can be queried per pair (`showBalance`), per user
   (`showBalancesFor`), or globally (`showAllBalances`) at any point.
6. A debtor settles up (`settleUp()`), which reduces what they owe their
   creditor; observers are notified of the settlement too.

## Design patterns used

- **Strategy** — `strategy/SplitStrategy.java` with `EqualSplitStrategy`,
  `ExactSplitStrategy`, and `PercentSplitStrategy`. Each `addExpense()` call
  picks the algorithm for turning a total amount into per-user shares;
  `Exact`/`Percent` also validate their inputs and throw
  `InvalidSplitException` when shares don't add up.
- **Factory** — `factory/SplitStrategyFactory.java`. Maps a `SplitType` enum
  to the right `SplitStrategy` implementation, so `SplitwiseService` never
  references the concrete strategy classes directly.
- **Observer** — `observer/ExpenseObserver.java` with `ConsoleNotifier`.
  `SplitwiseService` doesn't know or care how notifications are delivered;
  it just calls `onExpenseAdded()`/`onSettlement()` on whatever observers are
  registered. `ConsoleNotifier` writes through a caller-supplied
  `Consumer<String>` sink instead of printing directly, so `Main` can fold
  notifications into the same transcript it logs command results to.

## Balance bookkeeping

`repository/BalanceSheetRepository.java` stores a nested map
`balances[A][B] = amount B owes A` (negative means A owes B). Every write
goes through `adjust(creditor, debtor, amount)`, which updates both
directions together — `balances[A][B] == -balances[B][A]` holds by
construction, so there's no separate "netting" step and no way for the two
directions to drift out of sync.

## Structure

```
splitwise/
  src/
    model/       User, Split, SplitType, Expense
    strategy/    SplitStrategy family (Equal/Exact/Percent)
    factory/     SplitStrategyFactory
    observer/    ExpenseObserver + ConsoleNotifier
    repository/  UserRepository, BalanceSheetRepository (in-memory)
    exceptions/  UserNotFoundException, InvalidSplitException
    services/    SplitwiseService (the only public entry point)
    Main.java    Reads a command script, drives the service, writes a transcript
  test/
    input/scenario.txt   All 3 split types, a cyclic debt, a settlement, and error cases
    output/output.txt    Captured run transcript
  explainer/index.html   Interactive step-through: pick who paid and a split type, tap "Next step" to watch
                          the real addExpense/settleUp call chain execute with live values (open directly in a browser)
```

## Running it

```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

## Known gaps (flagged, not fixed)

- No persistence — all state is in-memory and lost on process exit.
- No support for "settle up in full" convenience call — the caller must
  already know the exact amount owed (typically read via `showBalance()`
  first).
- No concurrency control — `addExpense`/`settleUp` aren't synchronized, so
  two threads updating the same pair's balance could race.
- Currency is a raw `double`; fine for this exercise, but a real system
  would want a fixed-point/`BigDecimal` money type to avoid floating-point
  drift over many transactions (rounding is currently patched per-strategy
  by giving the last participant the remainder cent).
