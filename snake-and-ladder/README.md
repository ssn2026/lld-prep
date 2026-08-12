# Snake & Ladder

> **Mode: Learning** — this design (happy flow, class breakdown, pattern
> choices) was authored by Claude, not the user. See `CLAUDE.md` → "Mode:
> Learning" for what that implies about ownership of the decisions below.

A 100-cell board game for 2+ players: roll a die each turn, move forward
that many cells, slide down any snake you land on or climb any ladder,
and be the first to land on exactly 100.

## Happy flow

1. `BoardBuilder` accumulates snakes and ladders (validated: no two on the
   same cell, a snake's tail must be below its head, a ladder's top must be
   above its bottom) and `build()`s an immutable `Board`.
2. `SnakeAndLadderService` is constructed with that board, the player
   names, and a `DiceStrategy`.
3. `rollAndMove()` acts for whoever's turn it currently is: rolls the die,
   computes the tentative new position, and either leaves the player in
   place (overshooting past 100 is a no-op turn) or moves them — then
   checks whether that landing cell has a snake or ladder and applies the
   jump immediately, notifying listeners at each step.
4. Landing exactly on 100 ends the game; every subsequent `rollAndMove()`
   throws.

## Design patterns used

- **Builder** — `builder/BoardBuilder.java`. `Board` has no public
  constructor — the only way to get one is through the builder, which
  validates the whole configuration (no overlapping cells, correct
  head/tail and bottom/top ordering) at `build()` time rather than letting
  an invalid board silently exist.
- **Strategy** — `strategy/DiceStrategy.java` with `RandomDiceStrategy`
  (seeded, like `blackjack/`'s `DeckFactory`, so test runs are
  reproducible). Swapping in, say, a scripted/fixed-sequence dice for
  deterministic unit tests is a new class, not a branch inside the service.
- **Observer** — `observer/GameListener.java` with `ConsoleGameListener`.
  A single `rollAndMove()` call can fire the listener *twice* — once for
  the raw dice move, once more for the snake/ladder jump if the landing
  cell has one — which is exactly the kind of "notify at each meaningful
  state change, not just at the end" use Observer is for.

## Structure

```
snake-and-ladder/
  src/
    model/       Board, Player, MoveReason
    builder/     BoardBuilder
    strategy/    DiceStrategy + RandomDiceStrategy
    observer/    GameListener + ConsoleGameListener
    exceptions/  InvalidBoardConfigException, GameAlreadyWonException
    services/    SnakeAndLadderService (the only public entry point)
    Main.java    Reads a command script, drives the service, writes a transcript
  test/
    input/scenario.txt   Board-validation errors, then a full seeded 2-player
                          game (snake bites, ladder climbs, an overshoot,
                          a win) ending with the already-won guard
    output/output.txt    Captured run transcript, including every Observer
                          event (not just stdout -- see Main.java's
                          TranscriptGameListener)
  diagrams/
    generate.py                Data-only script that builds snake-and-ladder.drawio
    snake-and-ladder.drawio    Class diagram + 1 sequence diagram (rollAndMove()
                                landing on a snake)
  explainer/index.html   Interactive step-through: pick 2 or 3 players, then roll
                          the die repeatedly and watch the real board-position
                          logic (including snake/ladder jumps) play out on a live
                          boustrophedon-numbered board
```

## Running it

```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

Requires the VS Code "Extension Pack for Java" (Microsoft) for the
`.vscode/launch.json` run config to work — open the `snake-and-ladder/`
folder itself as the workspace root, then use the "Run Main (scenario.txt)"
config.

## Known gaps (flagged, not fixed)

- No persistence — all game state is in-memory and lost on process exit.
- No "exact roll to finish" variant enforcement beyond the overshoot
  no-op — some house rules also require an exact roll to *enter* certain
  zones; this design only enforces it at the finish line (cell 100).
- Turn order is a fixed round-robin with no skip-turn/extra-turn rules
  (e.g. "roll a 6, go again") — a real board's house rules often add these.
- `RandomDiceStrategy`'s default constructor requires an explicit seed
  (no unseeded/production variant is provided) — this repo's test-first
  approach favors reproducibility, but a real game would want a genuinely
  random default.
