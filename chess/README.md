# Chess

> **Mode: Learning** — this design (happy flow, class breakdown, pattern
> choices) was authored by Claude, not the user. See `CLAUDE.md` → "Mode:
> Learning" for what that implies about ownership of the decisions below.

A two-player chess engine that enforces standard piece movement, turn order,
check/checkmate/stalemate detection, capture, and pawn promotion, driven
entirely through `ChessGameService`.

## Happy flow

1. `ChessGameService` is constructed with the two `Player`s and lays out the
   standard starting position via `PieceFactory`.
2. A caller submits `makeMove(fromAlgebraic, toAlgebraic)`. The service
   rejects it early if `currentState.allowsMove()` is false (game already
   over), the square is empty, it's the wrong player's piece, the
   destination isn't in the piece's pseudo-legal move set, or the move
   would leave the mover's own king in check (`Board.isMoveLegal`).
3. The move is applied to the `Board` (capture removed, piece relocated,
   pawn promoted via `PieceFactory` if it reached the back rank), recorded
   as a `Move`, and the turn flips.
4. `GameState.evaluate(board, opponentColor)` recomputes the game's status
   from scratch — is the mover-to-be in check, and do they have any legal
   move at all — and returns the matching singleton state
   (`InProgressState` / `CheckState` / `CheckmateState` / `StalemateState`).
5. Registered `GameObserver`s are notified: always `onMove`, plus `onCheck`
   or `onGameOver` if the new state warrants it. Once the state is
   checkmate/stalemate, `allowsMove()` is false and further `makeMove` calls
   throw `GameOverException`.

## Design patterns used

- **Factory** — `factory/PieceFactory.java`. Builds the correct `Piece`
  subclass (`Pawn`/`Knight`/`Bishop`/`Rook`/`Queen`/`King`) from a
  `PieceType`, used both for the initial board setup and for pawn
  promotion, so callers never `new` a concrete piece class directly.
- **State** — `state/GameState.java` with `InProgressState`, `CheckState`,
  `CheckmateState`, `StalemateState`. `ChessGameService` holds a single
  `currentState` field and defers to `allowsMove()` to decide whether a
  submitted move is even considered — checkmate/stalemate freeze the game,
  the other two don't. All four states share one transition rule
  (`GameState.evaluate`, a static interface method) so the actual
  check/checkmate/stalemate math lives in exactly one place.
- **Observer** — `observer/GameObserver.java` with `ConsoleGameObserver`.
  `ChessGameService` notifies every registered observer after each move
  (`onMove`, plus `onCheck`/`onGameOver` when relevant) without needing to
  know who — or how many listeners — are watching.

Piece movement itself (`Piece.getPossibleMoves(Board)`, overridden per
subclass) is plain polymorphism, not counted as one of the three headline
patterns above — Parking Lot, Splitwise, and Movie Booking all used
Strategy for this kind of "swap the algorithm" need, so this problem
intentionally reaches for Factory/State/Observer instead to cover more of
`docs/TRACKER.md`'s pattern checklist.

## Legal-move filtering: Board owns the simulation

`Board.isMoveLegal(piece, dest)` and `Board.hasAnyLegalMove(color)` both
work by taking a deep `Board.copy()`, applying the candidate move to the
copy, and checking `isSquareAttacked` on the resulting king position. Both
`ChessGameService.makeMove` (validating a submitted move) and
`GameState.evaluate` (checkmate/stalemate detection, which needs "does this
color have *any* legal move at all") call into these same two methods —
neither reimplements the simulation.

## Structure

```
chess/
  src/
    model/       Piece hierarchy (Pawn/Knight/Bishop/Rook/Queen/King), Board,
                 Position, Player, Move, Color/PieceType/GameStatus enums
    factory/     PieceFactory
    state/       GameState + InProgressState/CheckState/CheckmateState/StalemateState
    observer/    GameObserver, ConsoleGameObserver
    exceptions/  InvalidMoveException, GameOverException
    services/    ChessGameService (the only public entry point)
    Main.java    Reads a command script, drives the service, writes a transcript
  test/
    input/scenario.txt   Illegal-move edge cases + Fool's Mate (fastest real checkmate)
    output/output.txt    Captured run transcript
  explainer/index.html   Interactive step-through: tap "Next step" to watch the real
                          ChessGameService.makeMove() call chain execute on a live 8x8 board
                          through Fool's Mate and its illegal-move edge cases (open directly in a browser)
```

## Running it

```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

VS Code: open the `chess/` folder as the workspace root and use the
"Run Main (scenario.txt)" launch config (requires the "Extension Pack for
Java" by Microsoft).

## Known gaps (flagged, not fixed)

- **No castling, no en passant, no threefold-repetition/50-move draws.**
  `King.getPossibleMoves` only returns the 8 adjacent squares; pawns only
  get single/double forward and diagonal capture. These are the standard
  simplifications for an LLD pass — adding them would mean threading extra
  history state (has the king/rook moved, was the last move a two-square
  pawn push) through `Board`, which the current design deliberately doesn't
  carry.
- **Pawn promotion defaults to Queen** if no `promotionChoice` is passed to
  `makeMove` — there's no "underpromotion" UI concept here, just an optional
  parameter.
- **No draw offers/resignation** — the only terminal states are checkmate
  and stalemate; a real client would need those as separate service calls.
- No persistence and no concurrency control, consistent with the other
  problems in this repo — all state is in-memory, single-threaded.
