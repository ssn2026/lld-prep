# CrickInfo — Simple Walkthrough

This file explains the whole codebase in plain language, in the order you
should read it. No prior context assumed.

---

## 1. The story in one paragraph

Two teams play a limited-overs cricket match. You set the teams, start the
match, and then feed in deliveries one at a time — a run, a wicket, a
wide, a no-ball. The system keeps score, counts overs, brings in the next
batsman after a wicket, and automatically notices when an innings has
ended (all out, overs used up, or — for the team batting second — the
target has been chased down). Once both innings are done, it compares the
two scores and announces a winner. That's the whole system: set up, feed
in balls one by one, let the system figure out everything else.

---

## 2. The one door you're allowed to knock on

`src/services/CricInfoService.java` is the **only** class anything outside
the package is meant to call.

| Method | What it does |
|---|---|
| `setTeams(teamAName, playersA, teamBName, playersB)` | Register both teams before anything else |
| `startMatch(oversLimit)` | Begin the match; team A always bats first |
| `recordBall(type, runs)` | Feed in one delivery (`"RUN"`, `"WICKET"`, `"WIDE"`, or `"NOBALL"`) |
| `startSecondInnings()` | Move to team B's innings once team A's is over |
| `getScorecard()` | A formatted summary of the match so far |
| `addListener(listener)` | Get notified of wickets, innings completions, and the final result |

---

## 3. Read the code in this order

### Step 1 — the building blocks (`src/model/`)

- **`Team.java`** — just a name and an unmodifiable list of player names.
  Nothing clever; `Collections.unmodifiableList(players)` in the
  constructor means once a `Team` is built, nobody can sneak a player in
  or out of its roster later.
- **`Match.java`** — holds both teams, the overs limit, and (once they
  exist) `innings1` and `innings2`, plus a `result` string that starts out
  `null` and gets filled in once the match ends. Plain data holder with
  getters/setters — no logic.
- **`MatchStatus.java`** — an enum: `NOT_STARTED`, `INNINGS_1`,
  `INNINGS_BREAK`, `INNINGS_2`, `COMPLETED`. This is the five phases a
  match moves through, always in that exact order.
- **`Innings.java`** — the one model class that actually *does* things,
  not just stores them. Read it closely, because most of the game's real
  logic lives here as small mutator methods:
  - `addRuns(amount)` — adds to the running total.
  - `swapEnds()` — swaps `strikerIndex` and `nonStrikerIndex`. Whoever was
    non-striker becomes the one facing the next ball.
  - `recordLegalBall()` — increments the ball count for the current over;
    once it hits 6, the over is complete: `completedOvers++`, the ball
    count resets to 0, **and `swapEnds()` is called** (ends always swap at
    the end of an over, regardless of the last ball's runs).
  - `recordWicket()` — increments `wickets`, and if there's still a
    player left on the bench (`nextBatsmanIndex < battingTeam.size()`),
    that next player comes in as the new striker.
  - `isAllOut()` — true once `wickets >= players.size() - 1`. This is the
    real cricket rule that the *last* batsman standing has no partner left
    to bat with, so the innings ends one wicket "early" relative to the
    full XI.
  - `isOversComplete()` / `hasReachedTarget()` — the other two ways an
    innings can end (overs used up, or — only meaningful when `target` is
    non-null, i.e. only in the second innings — the score has gone past
    what's needed to win).

  Notice `Innings` never decides *when* to call these methods or in what
  combination — that decision lives one layer up, in `command/`.

### Step 2 — one delivery, one object (`src/command/`)

This is the **Command** pattern. `BallCommand.java` is a one-method
interface:

```java
public interface BallCommand {
    void execute(Innings innings);
}
```

Each delivery type gets its own tiny class implementing it:

- **`RunsBallCommand`** — stores how many runs off the bat, and its
  `execute()` does three things in order: `addRuns(runs)`, then — only if
  `runs % 2 == 1` — `swapEnds()` (an odd number of runs means the batsmen
  cross, so whoever was on strike is now at the other end), then always
  `recordLegalBall()` (a run-scoring delivery is a legal ball, so it
  counts toward the over).
- **`WicketBallCommand`** — `recordWicket()` then `recordLegalBall()`. A
  dismissal still consumes a legal ball.
- **`WideBallCommand`** / **`NoBallBallCommand`** — both just
  `addRuns(1)`. Notice **neither one calls `recordLegalBall()`** — that's
  the whole point of an extra in cricket: it adds a run but doesn't count
  toward the six balls of the over. This repo deliberately simplifies both
  to "always exactly 1 extra run" (see the README's "Known gaps") rather
  than modeling runs scored off the bat on a no-ball.

Why bother with four classes instead of one method with an `if`/`switch`
inside it? Because `CricInfoService` (Step 5) never needs to know which
kind of delivery it's dealing with — it just calls `command.execute(innings)`
the same way every time, and each command already knows exactly what it
needs to do to the `Innings`. Adding a fifth delivery type (say, a bye)
means writing one new small class, not editing a big conditional.

### Step 3 — turning a string into a command (`src/factory/BallCommandFactory.java`)

One static method:

```java
public static BallCommand create(String type, int runs) {
    return switch (type) {
        case "RUN" -> new RunsBallCommand(runs);
        case "WICKET" -> new WicketBallCommand();
        case "WIDE" -> new WideBallCommand();
        case "NOBALL" -> new NoBallBallCommand();
        default -> throw new IllegalArgumentException("Unknown ball type: " + type);
    };
}
```

This is the **only** place in the codebase with a `switch` over delivery
type strings. `CricInfoService.recordBall()` calls this once per ball and
gets back the right `BallCommand` object — it never has to branch on the
type itself.

### Step 4 — the match's own phase lifecycle (`src/state/`)

This is the **State** pattern. `MatchState.java` is the interface every
phase implements:

```java
public interface MatchState {
    MatchStatus getStatus();
    default MatchState startFirstInnings() { throw new IllegalMatchOperationException(...); }
    default void requireInningsInProgress() { throw new MatchNotInProgressException(getStatus()); }
    default MatchState startSecondInnings() { throw new IllegalMatchOperationException(...); }
    default MatchState endInnings() { throw new IllegalMatchOperationException(...); }
}
```

Every method's *default* behavior is to throw. A concrete phase class only
overrides the specific moves that are legal from that phase — exactly the
same trick used by `todo-list/`'s `TaskState` and the ATM's `AtmState`.
Walk through the five phase classes and you can read the entire match
lifecycle just from what each one overrides:

- **`NotStartedState`** overrides `startFirstInnings()` → returns
  `Innings1State.INSTANCE`. Nothing else is legal yet — you can't record a
  ball or start a second innings before the match has even begun.
- **`Innings1State`** overrides `requireInningsInProgress()` as a no-op
  (so `recordBall()` is allowed) and `endInnings()` → returns
  `InningsBreakState.INSTANCE`.
- **`InningsBreakState`** overrides only `startSecondInnings()` → returns
  `Innings2State.INSTANCE`. You can't record a ball during the break
  (`requireInningsInProgress()` isn't overridden here, so it still throws).
- **`Innings2State`** — same shape as `Innings1State`, but its
  `endInnings()` returns `CompletedState.INSTANCE` instead (the *second*
  innings ending means the whole match is over, not just a break).
- **`CompletedState`** overrides nothing at all — every method call falls
  through to a throwing default. That's what makes it terminal: once the
  match is `COMPLETED`, there is no method call left on `MatchState` that
  succeeds.

Every one of these classes is a `private` constructor plus a single
`public static final INSTANCE` — a Singleton, because a phase object holds
no data of its own (unlike `Innings`, which does).

The comment at the top of `MatchState.java` is worth internalizing: this
match's `MatchState` is held **once**, directly on `CricInfoService`,
because there's exactly one match happening at a time — same shape as the
ATM's `AtmState`. Contrast that with `todo-list/`'s `TaskState`, which is
held per-`Task`, because a todo list has many independent lifecycles
running at once instead of one shared machine.

### Step 5 — who gets told when something happens (`src/observer/`)

```java
public interface MatchListener {
    void onWicketFallen(String battingTeam, int wickets, int runs);
    void onInningsComplete(String battingTeam, int runs, int wickets, String overs);
    void onMatchComplete(String resultSummary);
}
```

`ConsoleMatchListener` is the one shipped implementation — three
`println`s. This is the **Observer** pattern: `CricInfoService` has no
idea who's listening or how many listeners exist; it just loops over
whatever's registered and calls the right method (see
`notifyWicketFallen`/`notifyInningsComplete`/`notifyMatchComplete` near
the bottom of the service). `Main.java` actually registers a *second*
listener too (see Step 7), proving multiple independent observers can
coexist without the service changing at all.

### Step 6 — errors (`src/exceptions/`)

Two exception classes:
- **`MatchNotInProgressException`** — thrown by
  `MatchState.requireInningsInProgress()`'s default, when `recordBall()`
  is called during any phase other than `INNINGS_1`/`INNINGS_2`. Its
  constructor takes the current `MatchStatus` so the message reads
  naturally (`"Cannot record a ball while match status is NOT_STARTED"`).
- **`IllegalMatchOperationException`** — the more general one, thrown by
  the other three `MatchState` defaults (`startFirstInnings`,
  `startSecondInnings`, `endInnings`), and also thrown directly by
  `CricInfoService.startMatch()` if you try to start a match before both
  teams are set (see Step 7 — this particular check isn't part of the
  State pattern at all).

### Step 7 — the orchestrator (`src/services/CricInfoService.java`)

Now that you've seen every piece, read `recordBall()` — the method that
does the most:

```java
public void recordBall(String type, int runs) {
    state.requireInningsInProgress();
    Innings innings = currentInnings();
    BallCommand command = BallCommandFactory.create(type, runs);
    command.execute(innings);
    if (command instanceof WicketBallCommand) {
        notifyWicketFallen(innings);
    }
    if (innings.isAllOut() || innings.isOversComplete() || innings.hasReachedTarget()) {
        notifyInningsComplete(innings);
        state = state.endInnings();
        if (state.getStatus() == MatchStatus.COMPLETED) {
            computeAndNotifyResult();
        }
    }
}
```

Read this top to bottom as a checklist:
1. Ask the current `MatchState` whether recording a ball is even legal
   right now (throws if not — Step 4).
2. Figure out which `Innings` object is "current" — `currentInnings()`
   just checks whether the status is `INNINGS_1` or not, and returns
   `match.getInnings1()` or `match.getInnings2()` accordingly.
3. Build the right `BallCommand` via the factory (Step 3) and execute it
   against that innings (Step 2), which mutates runs/wickets/overs/ends.
4. If it was specifically a wicket, notify listeners separately — this
   check happens *after* `execute()`, so `innings.getWickets()` already
   reflects the new wicket count by the time the listener sees it.
5. Check all three innings-ending conditions. If **any** is true, notify
   listeners that the innings is over, then ask the state to transition
   (`endInnings()`). If that transition lands on `COMPLETED` — which only
   happens when it was the *second* innings that just ended — compute and
   announce the winner.

Two ordering details worth noticing in `startMatch()` and
`startSecondInnings()`:

```java
public void startMatch(int oversLimit) {
    if (teamA == null || teamB == null) {
        throw new IllegalMatchOperationException("Cannot start a match before both teams are set");
    }
    state = state.startFirstInnings();
    match = new Match(teamA, teamB, oversLimit);
    match.setInnings1(new Innings(teamA, teamB.getName(), oversLimit, null));
}
```

`state.startFirstInnings()` is called **before** `match` is created. If
the state guard were to throw (say, `startMatch()` was somehow called a
second time while a match was already in progress), the throw happens
before any of the match-creation lines run, so nothing gets half-built.
The same "validate/transition first, mutate second" ordering appears in
`startSecondInnings()`.

`computeAndNotifyResult()` is the last piece — it compares
`innings1.getRuns()` and `innings2.getRuns()`, and for a team-B win,
computes `wicketsInHand` as `(teamB.getPlayers().size() - 1) -
innings2.getWickets()` — the same "-1" logic as `isAllOut()`, since the
maximum wickets a team can lose is one less than a full XI.

### Step 8 — the runner (`src/Main.java`)

A test harness that reads `test/input/scenario.txt` line by line, drives
`CricInfoService`, and writes a transcript to `test/output/output.txt`.
Worth noting: it registers **two** listeners —

```java
service.addListener(new ConsoleMatchListener());
service.addListener(new TranscriptMatchListener(output));
```

`TranscriptMatchListener` is a small private class defined inside
`Main.java` itself, so listener events also land in the saved transcript
file, not just the terminal. It also prints the `"> " + line` echo
*before* calling `execute()`, so listener output (which fires
synchronously, mid-`execute()`) appears in the transcript in true
chronological order relative to the command that triggered it.

---

## 4. Picture of one full flow: a wicket, mid-over

```
Main.java (reads "BALL WICKET")
   |
   v
CricInfoService.recordBall("WICKET", 0)
   |  state.requireInningsInProgress()      <- Innings1State: no-op, legal
   |  innings = currentInnings()             <- match.getInnings1()
   |  command = BallCommandFactory.create("WICKET", 0)
   |       -> new WicketBallCommand()
   |  command.execute(innings)
   |       innings.recordWicket()            <- wickets++, next batsman on strike (if any left)
   |       innings.recordLegalBall()         <- legalBallsThisOver++
   |  command instanceof WicketBallCommand   -> true
   |       notifyWicketFallen(innings)       <- every MatchListener.onWicketFallen(team, wickets, runs)
   |  innings.isAllOut() / isOversComplete() / hasReachedTarget()  -> all false (still early in the innings)
   v
recordBall() returns
Main.java prints: "OK ball recorded: WICKET"
```

## 5. Picture of a second flow: the innings ends mid-over (overs used up)

```
Main.java (reads "BALL RUN 0", the 12th legal ball of a 2-over innings)
   |
   v
CricInfoService.recordBall("RUN", 0)
   |  command.execute(innings)
   |       innings.addRuns(0)
   |       innings.recordLegalBall()
   |            legalBallsThisOver hits 6 -> completedOvers++ (now 2), legalBallsThisOver=0, swapEnds()
   |  innings.isOversComplete()             -> completedOvers(2) >= oversLimit(2) -> TRUE
   |       notifyInningsComplete(innings)    <- "India 23/2 (2.0 overs)"
   |       state = state.endInnings()        <- Innings1State -> InningsBreakState.INSTANCE
   |       state.getStatus() == COMPLETED?   -> false (it was the FIRST innings) -> skip result
   v
recordBall() returns
Main.java prints: "OK ball recorded: RUN 0"
```

---

## 6. Reading the actual captured run (`test/output/output.txt`)

A few real lines from the run, annotated:

```
> BALL RUN 1
ERROR MatchNotInProgressException: Cannot record a ball while match status is NOT_STARTED
```

The very first line of the script tries to record a ball before anything
has been set up at all. `state` starts as `NotStartedState.INSTANCE`,
which never overrides `requireInningsInProgress()`, so it throws
immediately, exactly as designed.

```
> MATCH 2
ERROR IllegalMatchOperationException: Cannot start a match before both teams are set
```

This is the one guard that lives directly in `CricInfoService`, not in
the State pattern — `teamA`/`teamB` are still `null` at this point in the
script (the `TEAM` commands haven't run yet), so `startMatch()`'s own
`if` check catches it first.

```
> NEXTINNINGS
ERROR IllegalMatchOperationException: Cannot start the second innings from status INNINGS_1
```

The match has started (status is now `INNINGS_1`), but `Innings1State`
never overrides `startSecondInnings()` — only `InningsBreakState` does —
so this correctly falls through to the throwing default.

```
> BALL WICKET
  [listener] WICKET! India 1 down, 5 runs
OK ball recorded: WICKET
```

India's first wicket, after having scored `4 + 1 = 5` runs on the two
balls before it (you can see `BALL RUN 4` then `BALL RUN 1` immediately
above this in the file). The listener line correctly shows `1 down` and
`5 runs` — both already reflect the state *after* `command.execute()` ran,
since the wicket notification happens right after execution.

```
> BALL RUN 0
  [listener] innings complete: India 23/2 (2.0 overs)
OK ball recorded: RUN 0
```

India's innings ends here — 2 wickets down, but the real trigger is
`isOversComplete()` (2.0 overs reached), not `isAllOut()` (which would
need far more than 2 wickets against an 11-player roster). The final
score, 23/2, matches exactly what `SCORECARD` prints two lines later in
the file.

```
> BALL RUN 6
  [listener] innings complete: Australia 24/0 (0.4 overs)
  [listener] match complete: Australia won by 10 wicket(s)
OK ball recorded: RUN 6
```

This is the chase finishing **early** — Australia's target was 23 (they
needed 24 to win), and after four sixes in a row (`6+6+6+6=24`), the very
next ball crosses `hasReachedTarget()`'s `runs > target` check well
before the 2 overs are used up (`0.4 overs` shown — only 4 balls bowled).
Both `onInningsComplete` and `onMatchComplete` fire from the *same*
`recordBall()` call, in that order, exactly matching the code in Step 7.
`10 wicket(s)` comes from `wicketsInHand = (11 - 1) - 0`.

```
> BALL RUN 1
ERROR MatchNotInProgressException: Cannot record a ball while match status is COMPLETED
```

The very last line proves `CompletedState` really is terminal — the match
is over, and any further `recordBall()` call throws.

---

## 7. Things to try / test yourself

Edit `test/input/scenario.txt` and re-run:
```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

1. **Make a team all out instead of running out of overs.** Set a large
   `oversLimit` (say `50`) and feed in ten `BALL WICKET` commands in a
   row for one innings. On the tenth, `isAllOut()` should trip
   (`wickets(10) >= players.size()-1(10)`) even though far fewer than 50
   overs have been bowled.
2. **Score an odd number of runs on the last ball of an over and watch
   the ends "un-swap".** `RunsBallCommand` swaps ends for odd runs, and
   `recordLegalBall()` swaps ends again if that ball also completes the
   over — the two swaps cancel out, so the same batsman who was on strike
   stays on strike for the new over. Confirm by checking
   `getStrikerName()` isn't exposed to `Main.java` directly, so you'll
   need to add a temporary print or step through in a debugger to see it.
3. **Feed a `BALL` command with a type the factory doesn't know**, e.g.
   `BALL BYE 1` — expect an `IllegalArgumentException` from
   `BallCommandFactory.create()` (uncaught by `Main.java`'s catch block,
   since it only catches `IllegalMatchOperationException` and
   `MatchNotInProgressException` — a good example of an error path that
   *isn't* handled gracefully, worth noticing).
4. **Give the chasing team exactly the target, not one more.** If team B
   ends up with `runs == target` (not `runs > target`), the match should
   *not* end early — `hasReachedTarget()` requires strictly greater than,
   matching the real cricket rule that a tie needs the team batting second
   to fall exactly short, and a win needs strictly more runs.
5. **Try `SCORECARD` before `NEXTINNINGS`.** You should see only
   `innings1`'s summary — `getScorecard()` only appends `innings2`'s
   summary `if (match.getInnings2() != null)`.
