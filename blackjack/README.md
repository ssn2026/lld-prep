# Blackjack

> **Mode: Learning** — this design (happy flow, class breakdown, pattern
> choices) was authored by Claude, not the user. See `CLAUDE.md` → "Mode:
> Learning" for what that implies about ownership of the decisions below.

Single-table blackjack: deal two cards to each player and the dealer,
players hit or stand in turn, the dealer plays a fixed house strategy, and
every hand is settled against the dealer's final total.

## Happy flow

1. `BlackjackService.startRound(playerNames)` builds a freshly shuffled
   `Deck` (via `DeckFactory`), deals two cards to every player and the
   dealer, and settles each `Hand`'s initial state — `BLACKJACK` if the
   first two cards total 21, otherwise `ACTIVE`.
2. While a player's hand is `ACTIVE`, `hit(name)`/`stand(name)` either draw
   another card (busting past 21 or staying active) or lock the hand into
   `STANDING`.
3. Once every player's hand is settled, `playDealerTurn()` runs the house
   rule (`StandardDealerStrategy`: hit below 17, stand at 17+) against the
   dealer's own hand.
4. `getRoundResult()` compares each settled player hand against the
   dealer's settled hand — bust beats everything, a natural blackjack beats
   a regular 21, otherwise it's a straight total comparison (or a push).

## Design patterns used

- **State** — `state/HandState.java` (interface with throwing defaults)
  plus `ActiveState`/`StandingState`/`BustedState`/`BlackjackState`
  singletons, held per-instance on each `Hand` — same shape as
  `todo-list/`'s per-`Task` state (every hand has its own independent
  lifecycle), not the ATM/CrickInfo style of one state shared on the
  service. `BlackjackService` never branches on `HandStatus` itself; a
  `hit()`/`stand()` on a non-`ACTIVE` hand simply falls through to
  `HandState`'s throwing default.
- **Strategy** — `strategy/DealerPlayStrategy.java` with
  `StandardDealerStrategy`. The house rule ("hit below 17") is pulled out
  from `playDealerTurn()` entirely, so a different table's rule (e.g. hit
  on soft 17) is one new class, not a branch inside the service.
- **Factory** — `factory/DeckFactory.java` builds and shuffles the full
  52-card deck, keeping that construction loop out of `Deck` itself (same
  role as `parking-lot/`'s `ParkingSpotFactory`). Shuffled with a fixed
  seed by default so every test run deals identical cards.

## Structure

```
blackjack/
  src/
    model/       Suit, Rank, Card, Hand (owns its own HandState), Deck, Player, HandStatus
    state/       HandState + Active/Standing/Busted/BlackjackState
    strategy/    DealerPlayStrategy + StandardDealerStrategy
    factory/     DeckFactory
    exceptions/  IllegalHandActionException, PlayerNotFoundException,
                 RoundNotReadyException, EmptyDeckException
    services/    BlackjackService (the only public entry point)
    Main.java    Reads a command script, drives the service, writes a transcript
  test/
    input/scenario.txt   A full round (fixed-seed deal) covering a natural
                          blackjack, a push (dealer also blackjack), a hit
                          to 17 and stand, plus every guard/error path
    output/output.txt    Captured run transcript
  diagrams/
    generate.py       Data-only script that builds blackjack.drawio
    blackjack.drawio  Class diagram + 2 sequence diagrams (a hit causing a
                       bust, the dealer's turn + round result)
  explainer/index.html   Interactive step-through: deal a round, hit or stand,
                          play the dealer's turn, and watch the real State
                          transitions and result logic play out on a live table
```

## Running it

```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

Requires the VS Code "Extension Pack for Java" (Microsoft) for the
`.vscode/launch.json` run config to work — open the `blackjack/` folder
itself as the workspace root, then use the "Run Main (scenario.txt)" config.

## Known gaps (flagged, not fixed)

- No betting/chips/payouts — this models hand outcomes (`WIN`/`LOSE`/`PUSH`)
  only, not money changing hands.
- No hole card / hidden dealer card — both dealer cards are visible in the
  scoreboard from the moment they're dealt (a real table hides one until
  the dealer's turn).
- `DeckFactory`'s default shuffle uses a fixed seed for reproducible test
  runs; a real game would shuffle fresh (or reuse `createShuffledDeck(seed)`
  with a truly random seed) every round.
- Single deck, no reshuffling mid-round — a long round with many players
  hitting repeatedly could in principle exhaust the deck
  (`EmptyDeckException`), which a real shoe with multiple decks avoids.
- No splitting or doubling down — a hand is a single, non-branching set of
  cards from deal to settle.
