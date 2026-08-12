# Splitwise — Simple Walkthrough

This file explains the whole codebase in plain language, in the order you
should read it. No prior context assumed.

---

## 1. The story in one paragraph

A group of friends share expenses — one person pays for dinner, another
pays for groceries, another books a trip — and everyone needs to know who
owes whom, and how much, without manually doing the math every time. This
system lets you register users, record an expense ("Alice paid $300 for
dinner, split between Alice, Bob, and Charlie"), and have it automatically
figure out each person's share and update a running ledger of who owes whom.
You can ask the ledger for a balance between any two people, for one
person's balances with everyone, or for the whole group's balances at once.
When someone pays back what they owe, a settlement reduces that debt. The
tricky part this system solves is that debts move in a *cycle* just as
easily as in a straight line (Bob might owe Alice while Alice owes Charlie
while Charlie owes Bob, all at the same time) — the ledger just needs to
track pairwise numbers correctly and let the numbers speak for themselves.

---

## 2. The one door you're allowed to knock on

`src/services/SplitwiseService.java` is the **only** class anything outside
the package is meant to call. Everything else (`model`, `strategy`,
`factory`, `observer`, `repository`, `exceptions`) is a helper this class
uses internally.

| Method | What it does |
|---|---|
| `addUser(name, email)` | Register a new user, get back the `User` object (with a generated id) |
| `addObserver(observer)` | Add a listener that gets told about every expense/settlement |
| `addExpense(description, amount, paidByUserId, splitType, participantUserIds, shareInputs)` | Record an expense, split it, update the ledger, get back the `Expense` |
| `settleUp(payerUserId, payeeUserId, amount)` | Record a debt repayment, reducing what the payer owes the payee |
| `showBalance(userId1, userId2)` | Human-readable balance between exactly two users |
| `showBalancesFor(userId)` | Human-readable list of everything one user owes/is owed |
| `showAllBalances()` | Human-readable list of every non-zero pairwise balance in the system |

---

## 3. Read the code in this order

### Step 1 — the building blocks (`src/model/`)

- **`User.java`** — a name, an email, and an `id` that's a randomly
  generated `UUID` (not something the caller supplies). `equals`/`hashCode`
  are overridden to compare by `id`, which matters later: it's what lets
  `BalanceSheetRepository.adjust()` correctly detect "this is the same
  person paying themselves" via `creditor.equals(debtor)`.
- **`SplitType.java`** — an enum: `EQUAL`, `EXACT`, `PERCENT`. Says *how* an
  expense's amount should be divided among participants.
- **`Split.java`** — the *result* of dividing an expense: one `User` plus
  the exact dollar `amount` they owe for that expense. An `Expense` ends up
  holding a `List<Split>`, one per participant.
- **`Expense.java`** — the record of one shared expense: a description, the
  total `amount`, who `paidBy` it, which `SplitType` was used, and the
  computed `List<Split>`. Its id (`"E1"`, `"E2"`, ...) comes from a shared
  `static AtomicInteger SEQUENCE`, so ids are unique and increasing across
  every `Expense` ever created, not per-user.

**None of these classes contain any splitting logic themselves — `Split` is
just a (user, amount) pair, and `Expense` just stores whatever splits it was
handed. The actual math of "who owes how much" lives in `src/strategy/`,
next.**

### Step 2 — how an expense gets divided (`src/strategy/`)

- **`SplitStrategy.java`** — one method,
  `computeSplits(totalAmount, participants, shareInputs)`, returning a
  `List<Split>`. The `shareInputs` map is keyed by `User.id`; each
  implementation interprets it differently (or ignores it).
- **`EqualSplitStrategy.java`** — ignores `shareInputs` entirely. Divides
  `totalAmount` by the number of participants (`baseShare`), and gives that
  same amount to everyone *except the last participant in the list*, who
  instead gets `totalAmount - runningTotal` — i.e., whatever's left over.
  This matters because plain division can produce numbers with more than 2
  decimal places (e.g. $100 / 3 = $33.333...); rounding every share to 2
  decimals independently could leave the shares summing to something other
  than the original `totalAmount` by a cent or two. Giving the *remainder*
  to the last participant guarantees the splits always add up exactly.
- **`ExactSplitStrategy.java`** — expects `shareInputs` to already contain
  an exact dollar amount per participant. It looks up each participant's
  entry (throwing `InvalidSplitException` if one is missing), sums them all,
  and throws `InvalidSplitException` again if that sum doesn't match
  `totalAmount` within a small tolerance (`EPSILON = 0.01`, to allow for
  floating-point noise rather than requiring an exact bit-for-bit match).
- **`PercentSplitStrategy.java`** — expects `shareInputs` to hold a
  percentage per participant. First it sums all the percentages and throws
  `InvalidSplitException` if they don't add up to 100 (again within
  `EPSILON`). Then it computes each participant's dollar share as
  `totalAmount * percentage / 100`, using the same "last participant absorbs
  the rounding remainder" trick as `EqualSplitStrategy` so the shares always
  sum to exactly `totalAmount`.

This is the **Strategy pattern**: "how do we turn one total amount into a
list of per-person shares" is a completely swappable algorithm, chosen per
call to `addExpense`. `SplitwiseService` never needs an `if
(splitType == EQUAL) ... else if ...` chain — it just calls
`computeSplits` on whichever strategy it's handed.

### Step 3 — how the right strategy gets picked (`src/factory/SplitStrategyFactory.java`)

One static method, `getStrategy(SplitType)`, with a `switch` expression
mapping `EQUAL`/`EXACT`/`PERCENT` to a freshly created instance of the
matching strategy class. This is the **Factory pattern** — it's the single
place in the codebase that knows the concrete strategy class names;
`SplitwiseService` just asks for "the strategy for this `SplitType`" and
gets back the right object.

### Step 4 — who gets told about expenses (`src/observer/`)

- **`ExpenseObserver.java`** — an interface with two methods:
  `onExpenseAdded(expense)` and `onSettlement(payer, payee, amount)`.
- **`ConsoleNotifier.java`** — the one implementation shipped with the
  library. For `onExpenseAdded`, it announces the expense, then loops over
  every `Split` in it and prints "`X owes Y $Z`" for every participant
  *except* the person who paid (it skips the split where
  `split.getUser().equals(expense.getPaidBy())`, since a payer doesn't owe
  themselves). For `onSettlement`, it prints one "`payer paid payee
  $amount`" line.

  The interesting design choice here is the constructor:
  `ConsoleNotifier(Consumer<String> sink)`. Rather than calling
  `System.out.println` directly, it writes every message through a
  caller-supplied `Consumer<String>` — a "give me something and I'll hand it
  off" callback. This is why `Main.java` can construct it as
  `new ConsoleNotifier(line -> log(output, line))`: notifications get folded
  into the *same* transcript `Main` is already building for command results,
  instead of notifications and command output living on two separate,
  hard-to-interleave streams.

This is the **Observer pattern**: `SplitwiseService` doesn't know or care
what `ConsoleNotifier` does with the news — it just calls
`onExpenseAdded`/`onSettlement` on every registered `ExpenseObserver` and
moves on.

### Step 5 — where things are stored (`src/repository/`)

- **`UserRepository.java`** — wraps a `Map<String, User>`. `save(user)`
  stores it, `findById(userId)` looks it up and throws
  `UserNotFoundException` if the id isn't there, and `findAll()` returns
  every registered user.
- **`BalanceSheetRepository.java`** — the heart of the ledger. It stores a
  *nested* map: `balances.get(A).get(B)` = the amount B owes A (a negative
  number means the reverse — A owes B). The whole class has exactly one
  write path, `adjust(creditor, debtor, amount)`:
  ```java
  public void adjust(User creditor, User debtor, double amount) {
      if (creditor.equals(debtor) || amount == 0) {
          return;
      }
      bump(creditor.getId(), debtor.getId(), amount);
      bump(debtor.getId(), creditor.getId(), -amount);
  }
  ```
  Every call updates *both* directions of the relationship in the same
  method call — `balances[A][B]` goes up by `amount` while `balances[B][A]`
  goes down by the same `amount`, via the shared private `bump()` helper.
  Because there's no separate step where one side gets updated and the
  other side is calculated later, `balances[A][B] == -balances[B][A]` is
  always true by construction — there's no way for the two directions to
  drift out of sync, and no separate "netting" pass is ever needed. The
  early-return for `creditor.equals(debtor)` is what makes a payer's own
  share of their own expense a harmless no-op instead of polluting the
  ledger with "Alice owes Alice" entries.

### Step 6 — errors (`src/exceptions/`)

- `UserNotFoundException` — thrown by `UserRepository.findById` when a
  given user id doesn't exist.
- `InvalidSplitException` — thrown by `ExactSplitStrategy`/
  `PercentSplitStrategy` when shares are missing or don't add up correctly.

(A third failure case, an unrecognized user *name* at the `Main.java` script
level, surfaces as a plain `IllegalArgumentException` from `resolveUser` —
see Step 8 below — rather than a custom exception, since it's a test-harness
concern, not a `SplitwiseService` concern.)

### Step 7 — the orchestrator (`src/services/SplitwiseService.java`)

Now that you've seen every piece, this file just wires them together. Two
methods are worth reading closely — see the full traces in section 4 below:

- `addExpense` — looks up the payer and every participant, asks the
  factory for the right `SplitStrategy`, computes the splits, then calls
  `balanceSheet.adjust(paidBy, split.getUser(), split.getAmount())` **once
  per split** (including the payer's own split, if they're a participant —
  the repository's self-check quietly ignores that one).
- `settleUp` — calls `balanceSheet.adjust(payee, payer, -amount)`. Notice
  the *payee* is passed as the creditor and the amount is *negated* — this
  is because a settlement is the mirror image of an expense: paying down a
  debt should reduce, not increase, what the payer owes.

The three `show*` methods all end up calling the same private helper,
`formatBalance(a, b, amountBOwesA)`, which turns a raw signed number into a
sentence: positive means "b owes a," negative means "a owes b," and zero
means "settled up." `showBalancesFor`/`showAllBalances` both skip any
balance whose absolute value is below `EPSILON` (0.01) so that
floating-point leftovers don't get reported as a real debt.

### Step 8 — the runner (`src/Main.java`)

Not part of the "real" library — a test harness. It reads a text script
line by line (`test/input/scenario.txt`), turns each line into a call on
`SplitwiseService`, and writes everything (including notifications) to
`test/output/output.txt`. It keeps its own `usersByName` map so the script
can refer to people by name ("Alice") instead of by their generated UUID,
resolving names to `User` objects via the private `resolveUser` helper
(which throws `IllegalArgumentException` for an unknown name). For
`ADD_EXPENSE`, it parses a comma-separated participant list where each
token is either a bare name (`Alice`) or `name:value` (`Alice:50`) — the
`:value` part becomes an entry in the `shareInputs` map passed to
`addExpense`, used by `EXACT` and `PERCENT` but ignored by `EQUAL`.

---

## 4. Picture of one full flow: an expense, then a settlement

### `addExpense("Trip", 400, charlieId, PERCENT, [aliceId, bobId, charlieId], {alice:30, bob:20, charlie:50})`

```
Main.java (reads "ADD_EXPENSE Trip 400 Charlie PERCENT Alice:30,Bob:20,Charlie:50")
   |
   v
SplitwiseService.addExpense("Trip", 400, charlieId, PERCENT, [alice,bob,charlie], {alice:30,bob:20,charlie:50})
   |
   | userRepository.findById(charlieId)             -> paidBy = Charlie
   | userRepository.findById(...) for each id        -> participants = [Alice, Bob, Charlie]
   |
   | SplitStrategyFactory.getStrategy(PERCENT)        -> new PercentSplitStrategy()
   | strategy.computeSplits(400, [Alice,Bob,Charlie], {alice:30,bob:20,charlie:50})
   |     percentSum = 30+20+50 = 100                  -> OK, no InvalidSplitException
   |     Alice: 400 * 30 / 100 = 120
   |     Bob:   400 * 20 / 100 = 80
   |     Charlie (last participant): 400 - (120+80) = 200  <- remainder, not 400*50/100 directly
   |     -> splits = [Split(Alice,120), Split(Bob,80), Split(Charlie,200)]
   |
   | new Expense("Trip", 400, Charlie, PERCENT, splits)   -> id "E3"
   |
   | for each split:
   |     balanceSheet.adjust(Charlie, Alice, 120)      -> Alice now owes Charlie 120 more
   |     balanceSheet.adjust(Charlie, Bob, 80)         -> Bob now owes Charlie 80 more
   |     balanceSheet.adjust(Charlie, Charlie, 200)    -> creditor.equals(debtor) -> no-op
   |
   | for each registered ExpenseObserver:
   |     observer.onExpenseAdded(expense)
   |        -> ConsoleNotifier prints the expense line, then one "X owes Charlie $Y"
   |           line per split EXCEPT the one where the user equals the payer
   v
returns the Expense to Main.java, which prints:
"OK expense E3 'Trip' ($400.0, PERCENT) added"
```

### `settleUp(bobId, aliceId, 50)`

```
Main.java (reads "SETTLE Bob Alice 50")
   |
   v
SplitwiseService.settleUp(bobId, aliceId, 50)
   |
   | userRepository.findById(bobId)    -> payer = Bob
   | userRepository.findById(aliceId)  -> payee = Alice
   |
   | balanceSheet.adjust(payee=Alice, payer=Bob, -50)
   |     bump(Alice.id, Bob.id, -50)   -> balances[Alice][Bob] decreases by 50
   |     bump(Bob.id, Alice.id, +50)   -> balances[Bob][Alice] increases by 50
   |     (a settlement is just a negative expense-adjustment in the other direction)
   |
   | for each registered ExpenseObserver:
   |     observer.onSettlement(Bob, Alice, 50)
   |        -> ConsoleNotifier prints "Bob paid Alice $50.00"
   v
Main.java prints: "OK settled"
```

---

## 5. Reading the actual captured run (`test/output/output.txt`)

The scenario registers Alice, Bob, and Charlie, then records three
expenses — one of each `SplitType` — that deliberately create a *cyclic*
debt: Bob owes Alice, Alice owes Charlie, and Charlie owes Bob, all at once.

```
> ADD_EXPENSE Dinner 300 Alice EQUAL Alice,Bob,Charlie
  [NOTIFY] 'Dinner' ($300.00) added by Alice
  [NOTIFY] Bob owes Alice $100.00
  [NOTIFY] Charlie owes Alice $100.00
OK expense E1 'Dinner' ($300.0, EQUAL) added
```

$300 split three ways with `EqualSplitStrategy` gives $100 each (`baseShare
= round2(300/3) = 100`, and even the last-participant remainder logic lands
on the same $100 here since it divides evenly). Alice herself is a
participant too, but the notification only lists Bob and Charlie — Alice's
own $100 split is silently skipped because `split.getUser().equals(paidBy)`
in `ConsoleNotifier`, and its ledger update is a no-op for the same
self-equals reason in `BalanceSheetRepository.adjust`.

```
> ADD_EXPENSE Groceries 150 Bob EXACT Alice:50,Charlie:100
  [NOTIFY] 'Groceries' ($150.00) added by Bob
  [NOTIFY] Alice owes Bob $50.00
  [NOTIFY] Charlie owes Bob $100.00
OK expense E2 'Groceries' ($150.0, EXACT) added
```

Note Bob himself isn't a listed participant this time (only `Alice:50` and
`Charlie:100` appear after `EXACT`), so `ExactSplitStrategy` only ever sees
two participants — Bob's own $0 share simply doesn't exist as a `Split`.
$50 + $100 = $150 = the expense amount, so no `InvalidSplitException`.

```
> ADD_EXPENSE Trip 400 Charlie PERCENT Alice:30,Bob:20,Charlie:50
  [NOTIFY] 'Trip' ($400.00) added by Charlie
  [NOTIFY] Alice owes Charlie $120.00
  [NOTIFY] Bob owes Charlie $80.00
OK expense E3 'Trip' ($400.0, PERCENT) added
```

30 + 20 + 50 = 100, so the percentages check passes. $400 * 30% = $120,
$400 * 20% = $80, and Charlie's own 50% share ($200) is computed but never
printed or added to the ledger (self-equals no-op again), matching the
trace in section 4.

```
> SHOW_ALL_BALANCES
Bob owes Alice $50.00
Alice owes Charlie $20.00
Charlie owes Bob $20.00
```

This is the netted result of all three expenses. Working it through
`BalanceSheetRepository`'s signed-number convention: after Dinner, Bob owed
Alice $100. After Groceries, Alice owed Bob $50, which nets against that
same pair to leave "Bob owes Alice $50" (100 - 50) — exactly the comment in
`test/input/scenario.txt` above this block. Alice–Charlie nets from Dinner's
$100 (Charlie owed Alice) against Trip's $120 (Alice owed Charlie) to
"Alice owes Charlie $20" (120 - 100). Bob–Charlie nets from Groceries' $100
(Charlie owed Bob) against Trip's $80 (Bob owed Charlie) to "Charlie owes
Bob $20" (100 - 80). Three separate expenses, paid by three different
people, collapsed into three simple numbers — that's the payoff of updating
both directions on every `adjust()` call instead of keeping a running list
of unresolved IOUs.

```
> SETTLE Bob Alice 50
  [NOTIFY] Bob paid Alice $50.00
OK settled
> SHOW_ALL_BALANCES
Alice owes Charlie $20.00
Charlie owes Bob $20.00
> SHOW_BALANCE Alice Bob
Alice and Bob are settled up
```

Bob pays off exactly what he owed Alice ($50), and the Alice–Bob line
disappears entirely from `SHOW_ALL_BALANCES` afterward (it's now 0, and
`formatBalance` reports "settled up" rather than "$0.00"). The cyclic Alice
owes Charlie / Charlie owes Bob debts are untouched — settling one pair
never affects any other pair, since `adjust` only ever touches the two
users passed to it.

```
> ADD_EXPENSE BadTest 100 Alice EXACT Bob:40,Charlie:50
ERROR InvalidSplitException: Exact splits sum to 90.0 but expense amount is 100.0
> ADD_EXPENSE BadPercent 100 Alice PERCENT Bob:60,Charlie:60
ERROR InvalidSplitException: Percentages sum to 120.0 but must sum to 100
> SHOW_BALANCE Alice Dave
ERROR IllegalArgumentException: Unknown user: Dave
```

Three distinct failure paths: `ExactSplitStrategy` catching a sum that's $10
short of the expense amount, `PercentSplitStrategy` catching percentages
that add up to 120 instead of 100, and `Main.resolveUser` catching a name
("Dave") that was never registered with `ADD_USER` — note this last one is a
plain `IllegalArgumentException`, not one of the two custom exceptions,
because it's the test harness's own name-to-`User` lookup failing, not
`SplitwiseService` itself.

---

## 6. Things to try / test yourself

You can edit `test/input/scenario.txt` and re-run with:
```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

1. **Make an equal split that doesn't divide evenly.**
   `ADD_EXPENSE Coffee 10 Alice EQUAL Alice,Bob,Charlie` ($10 / 3 = $3.33...
   repeating). Check `SHOW_ALL_BALANCES` afterward — the three shares should
   still sum to exactly $10.00 thanks to the last-participant-absorbs-the-
   remainder trick in `EqualSplitStrategy`, even though $10/3 isn't a clean
   number.

2. **Settle more than is owed.**
   After `SETTLE Bob Alice 50` already zeroes that pair, add another
   `SETTLE Bob Alice 20`. Since nothing in `settleUp` checks against the
   current balance, this should push the balance *negative* — check
   `SHOW_BALANCE Alice Bob` afterward and see if it now reports "Alice owes
   Bob" instead (the reverse of before), proving there's no guard against
   overpaying.

3. **Add an expense where the payer isn't a participant at all.**
   `ADD_EXPENSE Taxi 60 Alice EXACT Bob:30,Charlie:30` (Alice pays but isn't
   in the split). Confirm Alice ends up owed money by both Bob and Charlie,
   with no "Alice owes Alice" artifact anywhere.

4. **Trigger `UserNotFoundException` instead of the harness's own
   `IllegalArgumentException`.** The current `SHOW_BALANCE Alice Dave` edge
   case fails in `Main.resolveUser` before `SplitwiseService` is even
   called (since "Dave" was never in `usersByName`). You'd need to call
   `SplitwiseService.showBalance` directly with a syntactically-valid but
   never-`addUser`-registered UUID to see `UserNotFoundException` fire from
   `UserRepository.findById` instead — a good way to see the difference
   between a harness-level lookup failure and a service-level one.

5. **Check the cyclic debt collapses further.**
   Continue the existing scenario with `SETTLE Alice Charlie 20` and
   `SETTLE Charlie Bob 20` — after both, `SHOW_ALL_BALANCES` should print
   "Everyone is settled up", proving the whole three-person cycle nets to
   zero once every pairwise debt is paid off.

6. **Break something on purpose.**
   Try `ADD_EXPENSE X 100 Alice PERCENT Bob:50` (only one participant listed
   for a percent split that should involve more people) or a `SplitType`
   that doesn't exist, like `ADD_EXPENSE X 100 Alice HALF Bob:50,Alice:50` —
   trace the resulting error back to `SplitType.valueOf` in `Main.execute`.
