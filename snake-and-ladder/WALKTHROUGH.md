# Snake & Ladder — Simple Walkthrough

This file explains the whole codebase in plain language, in the order you
should read it. No prior context assumed.

---

## 1. The story in one paragraph

A 100-cell board has some snakes and some ladders scattered across it.
Players take turns rolling a die and moving forward that many cells. Land
on a snake's head, and you slide back down to its tail. Land on a
ladder's bottom, and you climb up to its top. If a roll would carry you
past cell 100, nothing happens — you just lose that turn (you need to
land on 100 *exactly* to win). First player to land exactly on 100 wins,
and the game is over for good after that.

---

## 2. The one door you're allowed to knock on

`src/services/SnakeAndLadderService.java` is the **only** class anything
outside the package is meant to call.

| Method | What it does |
|---|---|
| `SnakeAndLadderService(board, playerNames, diceStrategy)` | Set up a new game |
| `rollAndMove()` | Roll for whoever's turn it is, move them, return the roll value |
| `getPositions()` | Every player's current cell |
| `getWinner()` | `null` until someone's won, then their name forever |
| `getCurrentPlayerName()` | Whose turn is it right now |
| `addListener(listener)` | Get notified of every position change and the win |

Building the board itself isn't a method on this service — it goes
through its own builder first, covered in Step 2.

---

## 3. Read the code in this order

### Step 1 — the plain data (`src/model/`)

- **`Player.java`** — a name and a mutable `position` (starts at `0`,
  meaning "not yet on the board" — cell 1 is the actual first square).
- **`MoveReason.java`** — an enum: `DICE_ROLL`, `SNAKE`, `LADDER`. This
  tags *why* a player's position just changed, which matters because a
  single dice roll can cause a player's position to change **twice** in a
  row (Step 6) — once for the roll itself, once more if they land on a
  jump.
- **`Board.java`** — holds the board `size` (always 100 in this repo) and
  a `jumps` map: `cell -> destination`, used for *both* snake heads and
  ladder bottoms alike — there's no separate "snakes map" and "ladders
  map". Read its one-line doc comment: *"Immutable; only ever constructed
  via builder.BoardBuilder."* There's a public constructor, but by
  convention the builder (Step 2) is the real front door.

### Step 2 — building a valid board (`src/builder/BoardBuilder.java`)

This is the **Builder** pattern, and here it earns its keep by doing real
validation, not just convenience chaining. Look at `addSnake()`:

```java
public BoardBuilder addSnake(int head, int tail) {
    validateCell(head);
    validateCell(tail);
    if (tail >= head) {
        throw new InvalidBoardConfigException("Snake tail (" + tail + ") must be below its head (" + head + ")");
    }
    addJump(head, tail);
    return this;
}
```

`validateCell()` checks the cell is strictly between 1 and the board size
(`cell <= 1 || cell >= size` throws) — cells 1 and 100 themselves can
never be a snake/ladder endpoint, since 1 is the start and 100 is the
finish line. Then it checks the snake actually points *downward*
(`tail >= head` is illegal — a "snake" that sends you up the board isn't
a snake). `addLadder()` is the mirror image: it checks `top <= bottom` is
illegal (a "ladder" that sends you down isn't a ladder).

Both call a shared private helper, `addJump()`:

```java
private void addJump(int from, int to) {
    if (jumps.containsKey(from)) {
        throw new InvalidBoardConfigException("Cell " + from + " already has a snake or ladder starting on it");
    }
    jumps.put(from, to);
}
```

This is what stops you from accidentally putting a snake's head on the
exact same cell as a ladder's bottom (or two snakes' heads on the same
cell) — every cell can be the *start* of at most one jump. Every one of
these checks fires the moment you call `addSnake()`/`addLadder()`, not
later at `build()` time — so a mistake is caught as early as possible,
right where it was made.

### Step 3 — the dice (`src/strategy/`)

```java
public interface DiceStrategy {
    int roll();
}
```

One implementation, `RandomDiceStrategy`:

```java
public RandomDiceStrategy(long seed) {
    this.random = new Random(seed);
}
public int roll() {
    return random.nextInt(6) + 1;
}
```

This is the **Strategy** pattern. Read the doc comment: seeded on
purpose, the same trick `blackjack/`'s `DeckFactory` uses for its
shuffle — a `java.util.Random` constructed with a fixed seed produces the
exact same sequence of "random" numbers every single time the program
runs, which is what makes `test/output/output.txt` (Step 7) reproducible.
A different `DiceStrategy` implementation — say, one that always returns
a fixed value for testing edge cases, or a "loaded die" for a joke mode —
is a drop-in replacement; `SnakeAndLadderService` never cares which one
it was handed, it just calls `.roll()`.

### Step 4 — who gets told when a player moves (`src/observer/`)

```java
public interface GameListener {
    void onPositionChanged(String playerName, int from, int to, MoveReason reason);
    void onGameWon(String playerName);
}
```

`ConsoleGameListener` is the one shipped implementation — two `println`s.
This is the **Observer** pattern: `SnakeAndLadderService` doesn't know or
care who's listening. The interesting detail is *how many times* a single
turn can call `onPositionChanged`: once for the raw dice move, and again
if that move lands on a snake or ladder (Step 6) — this is exactly the
"notify at each meaningful change, not just once at the end" use Observer
is good for.

### Step 5 — errors (`src/exceptions/`)

- **`InvalidBoardConfigException`** — thrown by `BoardBuilder` (Step 2)
  for any invalid snake/ladder configuration.
- **`GameAlreadyWonException`** — thrown by `rollAndMove()` if anyone
  tries to roll after the game already has a winner.

### Step 6 — the orchestrator (`src/services/SnakeAndLadderService.java`)

Everything comes together in one method, `rollAndMove()`:

```java
public int rollAndMove() {
    if (winner != null) {
        throw new GameAlreadyWonException(winner);
    }
    Player current = players.get(currentPlayerIndex);
    int roll = diceStrategy.roll();
    int from = current.getPosition();
    int tentative = from + roll;

    if (tentative > board.getSize()) {
        advanceTurn();
        return roll;
    }

    current.setPosition(tentative);
    notifyPositionChanged(current.getName(), from, tentative, MoveReason.DICE_ROLL);

    Integer jumpTo = board.getJumps().get(tentative);
    if (jumpTo != null) {
        MoveReason reason = jumpTo < tentative ? MoveReason.SNAKE : MoveReason.LADDER;
        current.setPosition(jumpTo);
        notifyPositionChanged(current.getName(), tentative, jumpTo, reason);
    }

    if (current.getPosition() == board.getSize()) {
        winner = current.getName();
        notifyGameWon(winner);
        return roll;
    }

    advanceTurn();
    return roll;
}
```

Read it as a sequence of checks:
1. **Already won?** Throw immediately — nothing else in this method ever
   runs for a completed game.
2. **Roll and compute a tentative position.** `tentative = from + roll`.
   Nothing is committed to `current` yet.
3. **Overshoot check.** If `tentative` would carry the player past cell
   100, the player doesn't move *at all* — but the turn still passes
   (`advanceTurn()` still runs) and the roll value is still returned, so
   the caller knows what was rolled even though nothing happened. No
   listener fires for this case at all — there was no position change to
   report.
4. **Commit the raw move and notify.** Only past the overshoot check does
   `current.setPosition(tentative)` actually happen, immediately followed
   by a `DICE_ROLL` notification.
5. **Check for a jump.** `board.getJumps().get(tentative)` — if the cell
   the player just landed on is a jump's starting cell, figure out
   whether it's a `SNAKE` (`jumpTo < tentative`, sending them backward) or
   a `LADDER` (`jumpTo > tentative`, sending them forward), move them
   again, and fire a **second** notification with the jump's reason. This
   is why a single `rollAndMove()` call can produce two
   `onPositionChanged` events.
6. **Win check.** Only after any jump has already been applied — so a
   ladder that happens to land exactly on 100 correctly counts as a win
   too — check whether the player's *final* position (post-jump, if any)
   equals the board size. If so, set `winner`, notify, and return early —
   notice `advanceTurn()` is deliberately **not** called in this branch,
   since there's no next turn to advance to.
7. **Otherwise, advance the turn** and return the roll value.

`advanceTurn()` itself is one line: `currentPlayerIndex = (currentPlayerIndex
+ 1) % players.size()` — simple round-robin, wrapping back to player 0
after the last player's turn.

### Step 7 — the runner (`src/Main.java`)

A test harness reading `test/input/scenario.txt`, driving
`SnakeAndLadderService`, and writing a transcript to
`test/output/output.txt`. It accumulates `BOARD SNAKE`/`BOARD LADDER`
commands into a `BoardBuilder` field *before* the `START` command calls
`.build()` on it — the script has to configure the whole board before the
game officially begins. It also registers **two** listeners at once,
`ConsoleGameListener` and a small `TranscriptGameListener` defined inside
`Main.java` itself, so listener events land in the saved transcript file
and not just the terminal.

---

## 4. Picture of one full flow: a roll that lands on a snake

```
Main.java (reads "ROLL", Alice currently at position 12)
   |
   v
SnakeAndLadderService.rollAndMove()
   |  winner == null                      -> proceed
   |  current = Alice
   |  roll = diceStrategy.roll()          -> 5
   |  from = 12, tentative = 12 + 5 = 17
   |  tentative(17) > board.getSize()(100) -> false, not an overshoot
   |  current.setPosition(17)
   |  notifyPositionChanged("Alice", 12, 17, DICE_ROLL)
   |  jumpTo = board.getJumps().get(17)    -> 4   (cell 17 is a snake's head)
   |       reason = 4 < 17 -> SNAKE
   |       current.setPosition(4)
   |       notifyPositionChanged("Alice", 17, 4, SNAKE)
   |  current.getPosition()(4) == board.getSize()(100)?  -> false
   |  advanceTurn()                        <- Bob's turn next
   v
rollAndMove() returns 5
Main.java prints: "OK Alice rolled 5"
```

## 5. Picture of a second flow: an overshoot

```
Main.java (reads "ROLL", Alice currently at position 96)
   |
   v
SnakeAndLadderService.rollAndMove()
   |  roll = diceStrategy.roll()          -> 5
   |  from = 96, tentative = 96 + 5 = 101
   |  tentative(101) > board.getSize()(100) -> TRUE
   |       advanceTurn()                   <- turn still passes to Bob
   |       return 5                        <- no position change, no notification at all
   v
Main.java prints: "OK Alice rolled 5"    (Alice is STILL at 96 -- nothing about this
                                           line alone tells you that; you'd need to
                                           check STATUS or the absence of a listener line)
```

---

## 6. Reading the actual captured run (`test/output/output.txt`)

```
> BOARD SNAKE 10 20
ERROR InvalidBoardConfigException: Snake tail (20) must be below its head (10)
> BOARD LADDER 50 30
ERROR InvalidBoardConfigException: Ladder top (30) must be above its bottom (50)
```

Both deliberately backwards — a "snake" pointing up and a "ladder"
pointing down — caught by `addSnake()`/`addLadder()`'s own direction
checks before either ever reaches `addJump()`.

```
> BOARD SNAKE 17 4
OK snake 17 -> 4
...
> BOARD LADDER 17 90
ERROR InvalidBoardConfigException: Cell 17 already has a snake or ladder starting on it
```

Cell 17 was already claimed by the snake registered a few lines earlier.
Trying to also start a ladder there is caught by `addJump()`'s
`containsKey` check.

```
> ROLL
  [listener] Alice: 12 -> 17 (DICE_ROLL)
  [listener] Alice: 17 -> 4 (SNAKE)
OK Alice rolled 5
```

This is the exact trace from Section 4 — two listener lines from one
`ROLL` command, the raw move then the snake bite.

```
> ROLL
  [listener] Alice: 6 -> 8 (DICE_ROLL)
  [listener] Alice: 8 -> 34 (LADDER)
OK Alice rolled 2
```

Same double-notification shape, this time a ladder climb — landing on
cell 8 (a ladder's bottom) sends Alice up to 34.

```
> ROLL
OK Alice rolled 5
```

Look closely — this line has **no `[listener]` line above it at all**.
By this point in the script Alice is at position 96 (visible a few lines
earlier: `Alice: 91 -> 96`), and `96 + 5 = 101` overshoots the board.
Exactly the overshoot trace from Section 5 — the roll happened, the turn
passed, but nothing moved and nothing was reported. This pattern repeats
two more times right after it (`OK Alice rolled 6` and two more
`OK Alice rolled 2`s, all with no listener line), because `99 + 6 = 105`,
`99 + 2 = 101`, and `99 + 2 = 101` all overshoot too — Alice is stuck
one bad roll away from 100 for four turns in a row before finally rolling
exactly what's needed.

```
> ROLL
  [listener] Alice: 99 -> 100 (DICE_ROLL)
  [listener] Alice WINS!
OK Alice rolled 1 -> WINNER: Alice
```

`99 + 1 = 100` — exactly the board size, landing precisely on the finish
cell. Both `onPositionChanged` and `onGameWon` fire from this same
`rollAndMove()` call, and `Main.java`'s own return-value formatting adds
the `-> WINNER: Alice` suffix (see `Main.java`'s `ROLL` case, which
checks `service.getWinner()` right after calling `rollAndMove()`).

```
> STATUS
STATUS
  Alice: 100
  Bob: 87
  winner: Alice
```

Confirms the final state — Alice sitting exactly on 100, Bob still short
at 87, and `winner` now permanently set.

```
> ROLL
ERROR GameAlreadyWonException: Game is already over -- Alice won
```

The very last line — any further roll after the game is won throws
immediately, before the dice is even touched.

---

## 7. Things to try / test yourself

Edit `test/input/scenario.txt` and re-run:
```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

1. **Try to register a jump that lands exactly on cell 1 or cell 100.**
   `BOARD LADDER 1 50` or `BOARD SNAKE 100 50` — both should fail
   `validateCell()`'s `cell <= 1 || cell >= size` check, since neither
   endpoint is allowed to be the start or finish cell.
2. **Chain a ladder into a snake head on purpose.** Configure a ladder
   whose *top* lands exactly on a different jump's *starting* cell (e.g.
   a ladder `5 -> 17` where 17 is already a snake's head from this
   scenario). Watch whether the code follows the second jump automatically
   or not — reading `rollAndMove()` closely (Step 6), the jump check only
   runs *once* per roll, so a ladder landing on a snake's head should
   **not** chain into a second jump the same turn; confirm this by
   tracing the listener output.
3. **Add a third player** (`START Alice,Bob,Carol 1`) and confirm
   `advanceTurn()`'s round-robin correctly cycles through all three
   before returning to Alice.
4. **Change the seed** in the `START` command (e.g. `START Alice,Bob 2`
   instead of `1`) and rerun — you should get a completely different but
   still fully reproducible sequence of rolls, proving the seed (not
   anything else) controls determinism.
5. **Count how many `ROLL` commands it actually takes to win** with a
   given seed, and try a smaller or larger board size by changing the
   `BoardBuilder(100)` construction in `Main.java` — a smaller board
   should produce a winner in noticeably fewer rolls.
