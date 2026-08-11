# CrickInfo

> **Mode: Learning** — this design (happy flow, class breakdown, pattern
> choices) was authored by Claude, not the user. See `CLAUDE.md` → "Mode:
> Learning" for what that implies about ownership of the decisions below.

A live ball-by-ball cricket scorer: set two teams, start a limited-overs
match, feed in deliveries one at a time, and the scoreboard, overs,
wickets, innings transitions, and final result all update themselves.

## Happy flow

1. `CricInfoService.setTeams(...)` then `startMatch(oversLimit)` create the
   `Match` and the first `Innings`, moving match phase `NOT_STARTED ->
   INNINGS_1`.
2. Every delivery goes through `recordBall(type, runs)`: the type
   (`RUN`/`WICKET`/`WIDE`/`NOBALL`) is turned into a `BallCommand` by
   `BallCommandFactory`, and that command mutates the current `Innings`
   (runs, wickets, legal-ball/over counting, strike rotation).
3. After every ball, the service checks whether the innings just ended
   (all out, overs used up, or — in the second innings — the target was
   chased down) and, if so, notifies listeners and transitions the match
   phase (`endInnings()`).
4. `startSecondInnings()` moves `INNINGS_BREAK -> INNINGS_2`, seeding the
   chasing team's target from the first innings' final score.
5. Once the second innings ends, the service compares both totals and
   announces the winner (or a tie).

## Design patterns used

- **Command** — `command/BallCommand.java` with `RunsBallCommand`,
  `WicketBallCommand`, `WideBallCommand`, `NoBallBallCommand`. Each
  delivery type is its own object that knows how to mutate an `Innings`;
  `CricInfoService.recordBall()` never branches on ball type itself, it
  just executes whatever command the factory handed back.
- **Factory** — `factory/BallCommandFactory.java` maps a ball-type string
  to the right `BallCommand`, so the one type-dispatch `switch` in the
  whole codebase lives in exactly one place (same role as
  `parking-lot/`'s `ParkingSpotFactory`).
- **State** — `state/MatchState.java` (interface with throwing defaults)
  plus `NotStartedState`/`Innings1State`/`InningsBreakState`/
  `Innings2State`/`CompletedState` singletons, held once on
  `CricInfoService` — same shape as the ATM's `AtmState` (one physical
  machine, one match in progress), the opposite of `todo-list/`'s
  per-`Task` state (many independent lifecycles). `recordBall()` never
  branches on `MatchStatus`; it calls `state.requireInningsInProgress()`
  and lets the current state decide.
- **Observer** — `observer/MatchListener.java` with `ConsoleMatchListener`.
  Wicket falls, innings completions, and match results are all reported
  the same way, without `CricInfoService` knowing who's listening.

## Structure

```
cricinfo/
  src/
    model/       Team, Innings, Match, MatchStatus
    command/     BallCommand + Runs/Wicket/Wide/NoBallBallCommand
    factory/     BallCommandFactory
    state/       MatchState + NotStarted/Innings1/InningsBreak/Innings2/CompletedState
    observer/    MatchListener, ConsoleMatchListener
    exceptions/  MatchNotInProgressException, IllegalMatchOperationException
    services/    CricInfoService (the only public entry point)
    Main.java    Reads a command script, drives the service, writes a transcript
  test/
    input/scenario.txt   Full 2-innings match incl. wickets, wides/no-balls, an
                          early-finish chase, and every guard/error path
    output/output.txt    Captured run transcript
  diagrams/
    generate.py      Data-only script that builds cricinfo.drawio
    cricinfo.drawio  Class diagram + 2 sequence diagrams (record a wicket ball,
                      auto end-of-innings)
  explainer/index.html   Interactive step-through: start a match, bowl deliveries
                          (runs/wicket/wide/no-ball) and watch the real Command +
                          State transitions play out on a live scoreboard
```

## Running it

```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

Requires the VS Code "Extension Pack for Java" (Microsoft) for the
`.vscode/launch.json` run config to work — open the `cricinfo/` folder
itself as the workspace root, then use the "Run Main (scenario.txt)" config.

## Known gaps (flagged, not fixed)

- No persistence — all match state is in-memory and lost on process exit.
- Only one match at a time (no per-match id/lookup) — matches
  `MatchState` being held once on the service, like the ATM.
- No bowler tracking (overs-per-bowler limits, economy, etc.) — only the
  batting side's innings is modeled.
- Extras are simplified: a wide/no-ball always adds exactly 1 run; runs
  scored off the bat on a no-ball, byes, and leg-byes aren't modeled.
- "All out" uses a fixed 11-player roster assumption
  (`wickets >= players.size() - 1`); a team supplied with a different
  roster size would still work correctly, but real cricket's XI is assumed
  throughout the test scenario.
- No run-outs (a wicket always dismisses the striker; the non-striker can
  never be the one given out).
