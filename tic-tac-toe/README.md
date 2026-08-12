# Tic-Tac-Toe

> **Mode: Learning** — this design (happy flow, class breakdown, pattern
> choices) was authored by Claude, not the user. See `CLAUDE.md` → "Mode:
> Learning" for what that implies about ownership of the decisions below.

A 3x3 (or NxN, if constructed that way) grid game: X and O alternate
placing marks in empty cells until one side completes a full row, column,
or diagonal, or the board fills up with no winner.

## Happy flow

1. `TicTacToeService(size, winningStrategy)` builds an empty `Board` and
   starts with `X` to move.
2. `makeMove(row, col)` checks the game is still in progress, checks the
   cell is in bounds and empty, places the current mark, and notifies
   listeners of the move.
3. It then asks the `WinningStrategy` whether *that specific move* just
   completed a line. If so, the game transitions to a terminal won state;
   if the board is now full with no winner, it transitions to a draw;
   otherwise the mark alternates and the game continues.

## Design patterns used

- **Strategy** — `strategy/WinningStrategy.java` with
  `LineWinningStrategy`. Win-checking is pulled out from the service
  entirely, and deliberately only examines the row, column, and (if
  applicable) both diagonals *through the cell that was just played* —
  O(size) — rather than rescanning the whole board after every move. A
  different win condition (e.g. a Gomoku-style "any 5 in a row" on a
  larger board) is a new class, not a rewrite of `makeMove()`.
- **State** — `state/GameState.java` (interface with a throwing default)
  plus `InProgressState`/`XWonState`/`OWonState`/`DrawState` singletons,
  held once on `TicTacToeService` — same shape as the ATM's `AtmState` and
  CrickInfo's `MatchState` (one game per service instance), the opposite
  of `todo-list/`'s per-`Task` state. `makeMove()` never branches on
  `GameStatus` itself; it calls `state.requireInProgress()` and lets the
  current state decide.
- **Observer** — `observer/GameListener.java` with `ConsoleGameListener`.
  Every move and every game-over transition is reported the same way,
  without the service knowing who's listening.

## Structure

```
tic-tac-toe/
  src/
    model/       Board, Mark, GameStatus
    strategy/    WinningStrategy + LineWinningStrategy
    state/       GameState + InProgress/XWon/OWon/DrawState
    observer/    GameListener + ConsoleGameListener
    exceptions/  InvalidMoveException, GameOverException
    services/    TicTacToeService (the only public entry point)
    Main.java    Reads a command script, drives the service, writes a transcript
  test/
    input/scenario.txt   A full game (X wins the top row) incl. an
                          out-of-bounds move, an occupied-cell move, and a
                          move attempted after the game is already over
    output/output.txt    Captured run transcript, including every Observer
                          event (see Main.java's TranscriptGameListener)
  diagrams/
    generate.py           Data-only script that builds tic-tac-toe.drawio
    tic-tac-toe.drawio    Class diagram + 1 sequence diagram (makeMove()
                           completing a winning line)
  explainer/index.html   Interactive step-through: tap any empty cell and watch
                          the real bounds/occupancy checks, win-condition check,
                          and State transition play out on a live 3x3 grid
```

## Running it

```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

Requires the VS Code "Extension Pack for Java" (Microsoft) for the
`.vscode/launch.json` run config to work — open the `tic-tac-toe/` folder
itself as the workspace root, then use the "Run Main (scenario.txt)" config.

## Known gaps (flagged, not fixed)

- No persistence — all game state is in-memory and lost on process exit.
- No AI/computer player — both marks are driven by the same `makeMove()`
  caller; there's no strategy for choosing a move, only for judging one.
- Board size is fixed at construction (`new TicTacToeService(3, ...)`) —
  no mid-game resize, and `LineWinningStrategy` assumes a standard
  "fill the whole row/column/diagonal" win condition rather than a
  configurable run-length (e.g. Gomoku's "5 in a row" on a 15x15 board).
- No undo — a placed mark can never be retracted.
