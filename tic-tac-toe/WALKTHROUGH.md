# Tic-Tac-Toe — Simple Walkthrough

This file explains the whole codebase in plain language, in the order you
should read it. No prior context assumed.

---

## 1. The story in one paragraph

X and O take turns placing their mark in an empty cell of a 3x3 grid.
After every single move, the game checks whether *that specific move*
just completed a full row, column, or diagonal — if so, whoever just
moved wins and the game is over. If nobody's won and every cell is now
full, it's a draw. Otherwise, the turn passes to the other mark and play
continues. That's the whole system.

---

## 2. The one door you're allowed to knock on

`src/services/TicTacToeService.java` is the **only** class anything
outside the package is meant to call.

| Method | What it does |
|---|---|
| `TicTacToeService(size, winningStrategy)` | Start a new empty game, X moves first |
| `makeMove(row, col)` | Place the current mark, alternate turns |
| `getStatus()` | `IN_PROGRESS`, `X_WON`, `O_WON`, or `DRAW` |
| `renderBoard()` | A text picture of the current grid |
| `addListener(listener)` | Get notified of every move and every game-over |

---

## 3. Read the code in this order

### Step 1 — the grid itself (`src/model/`)

- **`Mark.java`** — `X`, `O`, `EMPTY`. Three values, nothing else.
- **`GameStatus.java`** — `IN_PROGRESS`, `X_WON`, `O_WON`, `DRAW`. Just the
  label; the logic that decides which one applies lives in `state/` (Step
  3).
- **`Board.java`** — a `Mark[][] grid` plus a running `filledCount`. Read
  the constructor: it builds an `size x size` grid and fills every cell
  with `Mark.EMPTY` up front (`Arrays.fill`), so there's never a `null`
  cell to worry about anywhere else in the codebase. Its methods are all
  small and single-purpose: `isInBounds(row, col)` (a plain range check),
  `isEmpty(row, col)`, `place(row, col, mark)` (writes the cell **and**
  increments `filledCount` — the two always happen together, which is
  exactly what makes `isFull()` a cheap `filledCount == size * size`
  comparison instead of scanning the whole grid every time). `render()`
  builds a simple text picture, one row per line, `.` for empty cells and
  the mark's first letter otherwise.

Notice the constructor takes a `size` parameter — this board isn't
hardcoded to 3x3. `Main.java` happens to always construct a
`new TicTacToeService(3, ...)` (Step 6), but nothing about `Board` itself
assumes 3.

### Step 2 — deciding what counts as a win (`src/strategy/`)

```java
public interface WinningStrategy {
    boolean checkWinner(Board board, int row, int col, Mark mark);
}
```

One implementation, `LineWinningStrategy`. Read its doc comment first:
*"Checks only the row, column, and (if applicable) both diagonals that
pass through the just-played cell — O(size), not a full O(size^2) board
scan — since a move can only ever complete a line it's actually on."*
That's the key efficiency idea: after someone plays at `(row, col)`, the
*only* lines that could possibly have just been completed are the ones
running through that exact cell — there's no need to re-check every row
and column on the whole board, most of which weren't touched by this
move at all.

Walk through the method:

```java
boolean rowWin = true, colWin = true;
for (int i = 0; i < size; i++) {
    if (board.get(row, i) != mark) rowWin = false;
    if (board.get(i, col) != mark) colWin = false;
}
if (rowWin || colWin) return true;
```

One loop checks *both* the entire row `row` and the entire column `col`
at once, comparing every cell against `mark`. If either stays `true` all
the way through, that's a win.

```java
if (row == col) {
    boolean diagWin = true;
    for (int i = 0; i < size; i++) if (board.get(i, i) != mark) { diagWin = false; break; }
    if (diagWin) return true;
}
```

The main diagonal (top-left to bottom-right, cells where `row == col`)
only needs checking **if the move itself was on that diagonal** — that's
what the outer `if (row == col)` guards against: a move at, say, `(0,1)`
can never complete the main diagonal, so there's no point even looping
over it.

```java
if (row + col == size - 1) {
    boolean antiDiagWin = true;
    for (int i = 0; i < size; i++) if (board.get(i, size - 1 - i) != mark) { antiDiagWin = false; break; }
    if (antiDiagWin) return true;
}
```

Same idea for the anti-diagonal (top-right to bottom-left) — a cell is on
it exactly when `row + col == size - 1` (for a 3x3 board, that's cells
`(0,2)`, `(1,1)`, `(2,0)`).

This is the **Strategy** pattern: `TicTacToeService` never has any
win-checking logic of its own — it just hands the board and the last
move's coordinates to whatever `WinningStrategy` it was constructed with.
Swapping in a different rule (say, a Gomoku-style "any 5 in a row" on a
much bigger board) means writing one new class implementing the same
one method; `makeMove()` (Step 5) never changes.

### Step 3 — the game's own phase lifecycle (`src/state/`)

This is the **State** pattern. `GameState.java`:

```java
public interface GameState {
    GameStatus getStatus();
    default void requireInProgress() {
        throw new GameOverException(getStatus());
    }
}
```

Same shape as every other State-pattern problem in this repo: the default
behavior is to throw, and only the state that should actually allow
moves overrides it. `InProgressState` overrides `requireInProgress()` as
a no-op — the only state where that method doesn't throw. `XWonState`,
`OWonState`, and `DrawState` each override nothing beyond `getStatus()` —
all three are equally terminal; once any of them is current, every
`makeMove()` call throws `GameOverException`, and there's nothing that
distinguishes how "over" the game is from the state's own perspective
(the *reason* it ended is captured in which of the three terminal classes
was chosen, not in any extra behavior on the class itself).

The doc comment on `GameState` is worth reading: this state is held
**once**, directly on `TicTacToeService` — one game per service instance
— the same shape as the ATM's `AtmState` and CrickInfo's `MatchState`,
and the opposite of `todo-list/`'s per-`Task` `TaskState` (which is held
per-entity because a todo list has many independent lifecycles running at
once).

### Step 4 — who gets told when something happens (`src/observer/`)

```java
public interface GameListener {
    void onMove(Mark mark, int row, int col);
    void onGameOver(GameStatus result);
}
```

`ConsoleGameListener` is the one shipped implementation — two `println`s.
This is the **Observer** pattern: `TicTacToeService` fires `onMove` after
*every* successful placement, and `onGameOver` only once, right when the
game actually ends (Step 5).

### Step 5 — the orchestrator (`src/services/TicTacToeService.java`)

`makeMove()` is the one method that does everything:

```java
public void makeMove(int row, int col) {
    state.requireInProgress();
    if (!board.isInBounds(row, col)) throw new InvalidMoveException(row, col, "out of bounds");
    if (!board.isEmpty(row, col)) throw new InvalidMoveException(row, col, "cell already occupied");

    board.place(row, col, currentMark);
    notifyMove(currentMark, row, col);

    boolean won = winningStrategy.checkWinner(board, row, col, currentMark);
    if (won) {
        state = currentMark == Mark.X ? XWonState.INSTANCE : OWonState.INSTANCE;
        notifyGameOver(state.getStatus());
        return;
    }
    if (board.isFull()) {
        state = DrawState.INSTANCE;
        notifyGameOver(state.getStatus());
        return;
    }
    currentMark = currentMark == Mark.X ? Mark.O : Mark.X;
}
```

Read it top to bottom as a checklist:
1. **Is the game even still on?** `state.requireInProgress()` throws
   immediately if not — nothing else in this method runs for a finished
   game.
2. **Is the move itself legal?** Two separate checks, each with its own
   specific reason string passed into `InvalidMoveException` — "out of
   bounds" and "cell already occupied" are genuinely different failure
   reasons, and the exception message reflects exactly which one fired.
3. **Commit the move and notify.** `board.place(...)` happens *before*
   any win/draw checking — you have to actually place the mark before you
   can ask "did this complete a line?"
4. **Check for a win.** Only the *just-played* cell's lines matter here
   (Step 2) — if `winningStrategy.checkWinner(...)` returns `true`, pick
   `XWonState` or `OWonState` based on `currentMark`, notify, and
   **return early** — the turn never switches, because there's no next
   turn.
5. **Check for a draw**, but only if nobody just won — `board.isFull()`
   after a winning move is irrelevant, which is exactly why this check
   comes *after* the win check, not before or combined with it.
6. **Otherwise, switch marks** and let the method return normally — the
   game continues.

`getStatus()` and `renderBoard()` are both simple delegations — to
`state.getStatus()` and `board.render()` respectively.

### Step 6 — the runner (`src/Main.java`)

A test harness reading `test/input/scenario.txt`, driving
`TicTacToeService`, and writing a transcript to `test/output/output.txt`.
It constructs the service as `new TicTacToeService(3, new
LineWinningStrategy())` — always a standard 3x3 board in this repo's demo,
even though nothing in the design requires that size. It registers two
listeners — `ConsoleGameListener` and a small private
`TranscriptGameListener` defined inside `Main.java` itself — so listener
events land in the saved transcript file, not just the terminal.

---

## 4. Picture of one full flow: the winning move

```
Main.java (reads "MOVE 0 2", board already has X at (0,0)/(0,1), O at (1,1)/(2,2))
   |
   v
TicTacToeService.makeMove(0, 2)
   |  state.requireInProgress()          <- InProgressState: no-op, legal
   |  board.isInBounds(0,2)              -> true
   |  board.isEmpty(0,2)                 -> true
   |  board.place(0, 2, X)                <- filledCount++
   |  notifyMove(X, 0, 2)                 <- every GameListener.onMove(X, 0, 2)
   |  won = winningStrategy.checkWinner(board, 0, 2, X)
   |       row 0: (0,0)=X, (0,1)=X, (0,2)=X   -> rowWin stays true
   |       -> returns true
   |  state = X == X ? XWonState.INSTANCE : ...   <- XWonState.INSTANCE
   |  notifyGameOver(X_WON)                <- every GameListener.onGameOver(X_WON)
   |  return                               <- early return, currentMark never switches
   v
Main.java prints: "OK move (0,2) -> X_WON"
```

---

## 5. Reading the actual captured run (`test/output/output.txt`)

```
> MOVE 5 5
ERROR InvalidMoveException: Invalid move at (5,5): out of bounds
```

The very first move in the script is deliberately illegal — `(5,5)` is
outside a 3x3 board, so `board.isInBounds(5,5)` fails before anything
else is even checked.

```
> MOVE 0 0
  [listener] X played (0,0)
OK move (0,0) -> IN_PROGRESS
```

X always moves first (`currentMark` starts as `Mark.X` in the service's
field declaration). Status stays `IN_PROGRESS` — a single mark can't
complete any line yet.

```
> MOVE 0 0
ERROR InvalidMoveException: Invalid move at (0,0): cell already occupied
```

Trying the exact same cell again — this time `isInBounds` passes but
`isEmpty(0,0)` correctly fails, since X already placed there.

```
> MOVE 1 1
  [listener] O played (1,1)
OK move (1,1) -> IN_PROGRESS
> MOVE 0 1
  [listener] X played (0,1)
OK move (0,1) -> IN_PROGRESS
> MOVE 2 2
  [listener] O played (2,2)
OK move (2,2) -> IN_PROGRESS
> STATUS
STATUS IN_PROGRESS
  X X .
  . O .
  . . O
```

The board after five moves: X has `(0,0)` and `(0,1)` — one cell away
from completing the top row. O has `(1,1)` and `(2,2)` — already two
cells into the main diagonal, but it's X's turn next, not O's.

```
> MOVE 0 2
  [listener] X played (0,2)
  [listener] game over: X_WON
OK move (0,2) -> X_WON
```

Exactly the trace from Section 4 — X completes the top row, and **both**
`onMove` and `onGameOver` fire from this single `makeMove()` call, in
that order.

```
> STATUS
STATUS X_WON
  X X X
  . O .
  . . O
```

The final board, confirming the top row really is all `X`.

```
> MOVE 1 0
ERROR GameOverException: Cannot move -- game is already over (X_WON)
```

The last line proves `XWonState` really is terminal — any further
`makeMove()` call throws before touching the board at all.

---

## 6. Things to try / test yourself

Edit `test/input/scenario.txt` and re-run:
```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

1. **Force a draw.** Fill every cell without either player ever
   completing a row/column/diagonal (there's a well-known set of 9 moves
   that does this for 3x3 — work it out on paper first, or just try moves
   and watch `STATUS` after each one). Confirm the final status is
   `DRAW`, not `IN_PROGRESS` or a win.
2. **Win via the anti-diagonal specifically**, e.g. marks ending up at
   `(0,2)`, `(1,1)`, `(2,0)`. Confirm `LineWinningStrategy`'s
   `row + col == size - 1` branch is what catches it (the main-diagonal
   branch, guarded by `row == col`, would never fire for any of those
   three cells individually).
3. **Try a move after a draw**, not just after a win — confirm
   `DrawState` is equally terminal (`GameOverException`), same as
   `XWonState`/`OWonState`.
4. **Trace what happens if O had won instead.** Play a short game where O
   completes a line on an even-numbered move (O always moves second, so
   O's winning move is always the 4th, 6th, or 8th move of the game).
   Confirm the reported status is `O_WON`, and that `currentMark == X ?
   XWonState... : OWonState...` correctly picked the O branch.
5. **Construct a `TicTacToeService` with a different size directly**
   (this isn't reachable via `Main.java`'s command language, which always
   uses `3` — you'd need to edit `Main.java` itself). Try `new
   TicTacToeService(4, new LineWinningStrategy())` and confirm a 4-in-a-row
   on any of the 4 rows/columns/diagonals still triggers a win, proving
   `Board` and `LineWinningStrategy` really don't hardcode 3 anywhere.
