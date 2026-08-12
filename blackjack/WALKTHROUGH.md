# Blackjack — Simple Walkthrough

This file explains the whole codebase in plain language, in the order you
should read it. No prior context assumed.

---

## 1. The story in one paragraph

A round starts: every player and the dealer each get two cards. If your
first two cards total exactly 21, that's a natural blackjack — no more
decisions to make. Otherwise, each player decides, one at a time, whether
to `hit` (take another card) or `stand` (stop where they are). Once every
player has settled, the dealer plays a fixed house rule — hit anything
below 17, stop at 17 or above — and then every player's final hand is
compared against the dealer's to decide win, lose, or push (a tie).
That's the whole system: deal, let players act, let the dealer act by a
fixed rule, compare.

---

## 2. The one door you're allowed to knock on

`src/services/BlackjackService.java` is the **only** class anything
outside the package is meant to call.

| Method | What it does |
|---|---|
| `startRound(playerNames)` | Fresh shuffled deck, deals 2 cards to every player and the dealer |
| `hit(playerName)` | That player draws one more card |
| `stand(playerName)` | That player locks their hand in as-is |
| `playDealerTurn()` | Runs the dealer's fixed hit/stand rule |
| `getRoundResult()` | Compares every settled hand against the dealer's |
| `getHandsSummary()` | Look at every hand's cards/total/status right now, mid-round |

---

## 3. Read the code in this order

### Step 1 — cards themselves (`src/model/Suit.java`, `Rank.java`, `Card.java`)

`Suit` is a plain 4-value enum. `Rank` is more interesting — it's an enum
where **each constant carries its own data**:

```java
public enum Rank {
    TWO(2), THREE(3), ..., TEN(10), JACK(10), QUEEN(10), KING(10), ACE(11);
    private final int value;
    Rank(int value) { this.value = value; }
    public int getValue() { return value; }
}
```

`TWO` through `TEN` are worth their face value, all three face cards are
worth 10, and `ACE` defaults to 11 (its value gets adjusted down
elsewhere — see `Hand.getTotal()` below). `Card` just pairs a `Suit` with
a `Rank`; nothing else.

### Step 2 — building and shuffling a deck (`src/factory/DeckFactory.java`)

This is the **Factory** pattern — the same role `parking-lot/`'s
`ParkingSpotFactory` plays, just for a completely different kind of
object. `createShuffledDeck()` loops over every `Suit` × every `Rank` (4
× 13 = 52 combinations), builds one `Card` per combination, and shuffles
the resulting list with `Collections.shuffle(cards, new Random(seed))`.

The comment on `DEFAULT_SEED = 7L` is worth reading twice: this repo
deliberately uses a **fixed** seed by default, meaning every single time
you run the program, you get the *exact same* shuffle, the *exact same*
cards dealt to the *exact same* players. That's not an oversight — it's
what makes `test/output/output.txt` (Step 6) reproducible: anyone who
recompiles and reruns this program will see the identical hands this
walkthrough quotes. A real casino table would instead call
`createShuffledDeck(someTrulyRandomSeed)`.

### Step 3 — a hand and its own lifecycle (`src/model/Hand.java` + `src/state/`)

`Hand.java` holds a `List<Card>` and, importantly, its own `HandState`
field, defaulting to `ActiveState.INSTANCE`. This is the **State**
pattern, and — just like `todo-list/`'s `TaskState` — it's held
**per-instance**: every `Hand` (every player's, and the dealer's too, since
`Player` is used for both — see Step 4) has its own independent state,
because a round has many hands all progressing independently, not one
shared machine.

`HandState.java` is the interface:

```java
public interface HandState {
    HandStatus getStatus();
    default void requireActive() { throw new IllegalHandActionException(getStatus(), "hit/stand on"); }
    default HandState hit(Hand hand, Card newCard) { throw new IllegalHandActionException(getStatus(), "hit"); }
    default HandState stand(Hand hand) { throw new IllegalHandActionException(getStatus(), "stand"); }
}
```

Same trick as every other State-pattern problem in this repo: every
method's default just throws, and a concrete state only overrides what's
actually legal from there. Only **`ActiveState`** overrides anything real:

```java
public HandState hit(Hand hand, Card newCard) {
    hand.addCard(newCard);
    return hand.getTotal() > 21 ? BustedState.INSTANCE : ActiveState.INSTANCE;
}
public HandState stand(Hand hand) {
    return StandingState.INSTANCE;
}
```

Notice `hit()` takes the *new card* as a parameter — it adds it to the
hand itself, then decides the resulting state based on the hand's new
total. `StandingState`, `BustedState`, and `BlackjackState` are all
terminal: none of them override anything, so once a hand reaches any of
those three, every further `hit`/`stand` call throws
`IllegalHandActionException`.

Back in `Hand.java`, `getTotal()` is the soft-ace scoring algorithm:

```java
public int getTotal() {
    int total = 0, aces = 0;
    for (Card card : cards) {
        total += card.getRank().getValue();
        if (card.getRank() == Rank.ACE) aces++;
    }
    while (total > 21 && aces > 0) {
        total -= 10;
        aces--;
    }
    return total;
}
```

Every ace starts counted as 11. If the running total busts past 21 *and*
there's still an ace being counted as 11, downgrade one ace to 1 (subtract
10) and check again — repeat until either the total is legal again or
every ace has already been downgraded. This is why an ace + a 6 is a
"soft 17" (11+6=17) that can still safely take another card without
necessarily busting, unlike a hard 17.

`settleInitialState()` is called once, right after the two starting cards
are dealt: `getTotal() == 21 ? BlackjackState.INSTANCE : ActiveState.INSTANCE`.
This is **not** a transition through `HandState.hit()`/`stand()` — there's
no "previous state" to transition from at deal time, so the service just
assigns the initial state directly (same pattern `todo-list/`'s `Task`
constructor uses for `TodoState.INSTANCE`).

### Step 4 — a player, and the dealer is just a player (`src/model/Player.java`)

```java
public class Player {
    private final String name;
    private final Hand hand = new Hand();
}
```

That's the whole class. There is deliberately no separate `Dealer` class
— `BlackjackService` just creates one more `Player` named `"Dealer"` (see
Step 6) and treats it exactly like any other player for hand-holding
purposes. The *only* thing that makes it special is that the dealer's
turn is driven by a `DealerPlayStrategy` instead of external `hit`/`stand`
calls (Step 5).

### Step 5 — the house rule (`src/strategy/`)

```java
public interface DealerPlayStrategy {
    boolean shouldHit(Hand dealerHand);
}
```

One implementation, `StandardDealerStrategy`:

```java
public boolean shouldHit(Hand dealerHand) {
    return dealerHand.getTotal() < 17;
}
```

This is the **Strategy** pattern: the rule "hit below 17, stand at 17+"
is completely pulled out of the service. A different table's house rule
(e.g. "hit on soft 17 too") would be a new class implementing the same
one-method interface — `playDealerTurn()` (Step 6) never has to change.

### Step 6 — the orchestrator (`src/services/BlackjackService.java`)

`startRound()` builds everything fresh each time — a brand-new shuffled
deck, a `LinkedHashMap<String, Player>` for the named players (in
insertion order, since a `HashMap` alone wouldn't preserve the order
players were given in), and one more `Player` for `"Dealer"`. It then
deals two cards to everyone via the private `dealInitialHand()` helper,
calling `settleInitialState()` on each hand right after.

`hit()` is short but the ordering matters:

```java
public void hit(String playerName) {
    Hand hand = findPlayer(playerName).getHand();
    hand.getState().requireActive();
    Card card = deck.draw();
    hand.setState(hand.getState().hit(hand, card));
}
```

`requireActive()` is called **before** `deck.draw()`. If the hand isn't
currently `ACTIVE`, this throws immediately, and the deck is never
touched — no card gets wastefully drawn from an illegal `hit()` call.
Only once the guard passes does a card actually leave the deck.

`playDealerTurn()`:

```java
public void playDealerTurn() {
    requireAllPlayersDone();
    Hand dealerHand = dealer.getHand();
    while (dealerHand.getState().getStatus() == HandStatus.ACTIVE && dealerStrategy.shouldHit(dealerHand)) {
        Card card = deck.draw();
        dealerHand.setState(dealerHand.getState().hit(dealerHand, card));
    }
    if (dealerHand.getState().getStatus() == HandStatus.ACTIVE) {
        dealerHand.setState(dealerHand.getState().stand(dealerHand));
    }
}
```

`requireAllPlayersDone()` first — the dealer can't play until every
*player* has finished hitting/standing/busting/blackjack-ing. The loop
condition checks **two things**: the dealer's hand must still be `ACTIVE`
(a dealer dealt a natural blackjack is already terminal and should never
enter this loop at all) *and* the strategy says to hit. Once the loop
exits — either the dealer's total reached 17+, or the dealer busted — the
final `if` only calls `stand()` if the hand is *still* `ACTIVE` (i.e. it
stopped because of the 17+ rule, not because it busted, since a busted
hand's `HandState` doesn't accept a `stand()` call anyway — it's already
terminal).

`getRoundResult()` and its helper `outcome()` are worth reading together.
`outcome()` checks conditions in a very specific order — bust beats
everything, a natural blackjack beats a plain 21, and only after both of
those is it a plain number comparison:

```java
if (playerStatus == BUSTED) return "LOSE";
if (dealerStatus == BUSTED) return "WIN";
if (playerStatus == BLACKJACK && dealerStatus == BLACKJACK) return "PUSH";
if (playerStatus == BLACKJACK) return "WIN";
if (dealerStatus == BLACKJACK) return "LOSE";
// otherwise: plain total comparison, PUSH if equal
```

Notice this order matters: if you checked "total comparison" before
checking `BLACKJACK`, a player's natural 21 wouldn't be distinguished
from a dealer's 21 reached via three cards — but blackjack rules say a
2-card 21 is special and *pushes* against another 2-card 21 rather than
just tying on total value (which happens to be the same outcome here
since both are 21, but the `BLACKJACK`-vs-`BLACKJACK` check exists
precisely to keep that distinction correct if this design were extended,
e.g. to pay blackjacks differently).

`requireAllPlayersDone()` is a small private helper reused by both
`playDealerTurn()` and `getRoundResult()` — a nice example of pulling a
repeated guard out into one place instead of duplicating the loop twice.

### Step 7 — errors (`src/exceptions/`)

- `IllegalHandActionException` — thrown by `HandState`'s throwing
  defaults (hit/stand on a non-`ACTIVE` hand).
- `PlayerNotFoundException` — thrown by `findPlayer()` for an unknown
  name.
- `RoundNotReadyException` — thrown by `requireAllPlayersDone()` (someone
  still has to act) and separately by `getRoundResult()` if the dealer
  specifically hasn't played yet.
- `EmptyDeckException` — thrown by `Deck.draw()` if the deck runs out; a
  single 52-card deck with only a couple of players in a demo round is
  never realistically going to hit this, but it's there as a safety net.

### Step 8 — the runner (`src/Main.java`)

A test harness reading `test/input/scenario.txt`, driving
`BlackjackService`, and writing `test/output/output.txt`. Its `HANDS`
command calls `getHandsSummary()` — a read-only, mid-round inspection
method that exists specifically so the test script (and a real UI) can
show every hand's cards/total/status at any point, not just at the very
end.

---

## 4. Picture of one full flow: a natural blackjack meets a chasing hand

Because `DeckFactory` uses a fixed seed, this is the **exact** deal every
run produces for `ROUND Alice,Bob`:

```
BlackjackService.startRound(["Alice", "Bob"])
   |  deck = DeckFactory.createShuffledDeck()      <- same 52 cards, same order, every run
   |  dealInitialHand(Alice)  ->  [ACE-SPADES, JACK-SPADES]  = 21  -> settleInitialState() -> BlackjackState
   |  dealInitialHand(Bob)    ->  [TEN-CLUBS, FOUR-HEARTS]   = 14  -> settleInitialState() -> ActiveState
   |  dealInitialHand(Dealer) ->  [KING-HEARTS, ACE-CLUBS]   = 21  -> settleInitialState() -> BlackjackState
   v
Alice and the Dealer are BOTH already terminal (BlackjackState) before a single
hit() or stand() call happens. Only Bob is ACTIVE and has any decisions left.


Main.java (reads "HIT Bob")
   |
   v
BlackjackService.hit("Bob")
   |  hand.getState().requireActive()     <- ActiveState: no-op, legal
   |  card = deck.draw()                   <- THREE-HEARTS
   |  hand.setState(hand.getState().hit(hand, card))
   |       ActiveState.hit(hand, card): hand.addCard(card); total is now 17 -> not >21 -> stays ActiveState
   v
Bob's hand: [TEN-CLUBS, FOUR-HEARTS, THREE-HEARTS] = 17 (ACTIVE)


Main.java (reads "STAND Bob")
   |
   v
BlackjackService.stand("Bob")
   |  hand.setState(hand.getState().stand(hand))   <- ActiveState.stand() -> StandingState.INSTANCE
   v
Bob's hand: 17 (STANDING)   <- now every player is settled


Main.java (reads "DEALER")
   |
   v
BlackjackService.playDealerTurn()
   |  requireAllPlayersDone()             <- Alice: BLACKJACK (not ACTIVE), Bob: STANDING (not ACTIVE) -> passes
   |  while (dealerHand ACTIVE && shouldHit(dealerHand))
   |       dealerHand.getState().getStatus() == ACTIVE?  -> FALSE, dealer is already BlackjackState
   |       loop body never runs at all
   |  if (dealerHand ACTIVE) stand(...)    <- also false, nothing to do
   v
Dealer's hand stays exactly as dealt: 21 (BLACKJACK)


Main.java (reads "RESULT")
   |
   v
BlackjackService.getRoundResult()
   |  requireAllPlayersDone()              <- passes
   |  dealerHand status != ACTIVE          <- passes
   |  outcome(Alice's hand, dealer's hand): both BLACKJACK -> "PUSH"
   |  outcome(Bob's hand, dealer's hand):   Bob=17/STANDING, Dealer=21/BLACKJACK
   |       not BUSTED, not BUSTED, not both BLACKJACK, not player BLACKJACK,
   |       dealer IS BLACKJACK -> "LOSE"
   v
"Dealer: 21 (BLACKJACK)\nAlice: 21 (BLACKJACK) -> PUSH\nBob: 17 (STANDING) -> LOSE"
```

---

## 5. Reading the actual captured run (`test/output/output.txt`)

```
> ROUND Alice,Bob
OK round started for [Alice, Bob]
> HANDS
HANDS
  Dealer: [KING-HEARTS, ACE-CLUBS] = 21 (BLACKJACK)
  Alice: [ACE-SPADES, JACK-SPADES] = 21 (BLACKJACK)
  Bob: [TEN-CLUBS, FOUR-HEARTS] = 14 (ACTIVE)
```

This confirms the fixed-seed deal exactly as traced in Section 4 — two
natural blackjacks dealt on the very first round, every single time this
program runs.

```
> DEALER
ERROR RoundNotReadyException: Bob still has to hit or stand
```

Even though the dealer's own hand is already `BLACKJACK` (terminal), you
still can't call `playDealerTurn()` yet — `requireAllPlayersDone()`
checks every *player*, and Bob is still `ACTIVE`. This is exactly the
kind of bug a thorough test script catches: an earlier version of this
code only checked whether the *dealer's* hand was `ACTIVE` before letting
`getRoundResult()` proceed, which the dealer's own natural blackjack would
have accidentally satisfied — masking the fact that Bob hadn't acted yet.

```
> HIT Ghost
ERROR PlayerNotFoundException: No player found with name: Ghost
```

A simple, direct proof that `findPlayer()`'s lookup guard works — "Ghost"
was never one of the names passed to `ROUND`.

```
> RESULT
ERROR RoundNotReadyException: Bob still has to hit or stand
```

Same guard as the earlier `DEALER` attempt, now applied to
`getRoundResult()` instead of `playDealerTurn()` — both call the same
`requireAllPlayersDone()` helper.

```
> HIT Alice
ERROR IllegalHandActionException: Cannot hit/stand on a hand that is BLACKJACK
```

Alice was dealt a natural blackjack and is therefore already terminal.
`BlackjackState` overrides nothing, so this correctly falls through to
`HandState`'s throwing default.

```
> RESULT
RESULT
  Dealer: 21 (BLACKJACK)
  Alice: 21 (BLACKJACK) -> PUSH
  Bob: 17 (STANDING) -> LOSE
```

Matches the trace in Section 4 exactly: Alice's blackjack pushes against
the dealer's blackjack, and Bob's 17 loses to the dealer's 21.

---

## 6. Things to try / test yourself

Edit `test/input/scenario.txt` and re-run:
```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

1. **Force a bust.** Keep `HIT`ting the same player past 21 (with this
   fixed seed, check `HANDS` after each hit to see the running total) and
   confirm the hand's status flips to `BUSTED`, and that a further `HIT`
   on it throws `IllegalHandActionException`.
2. **Change `DeckFactory.DEFAULT_SEED`** to a different `long` value,
   recompile, and rerun — you should get a completely different (but
   still fully deterministic, run after run) deal. This is a good way to
   convince yourself the seed really is what controls reproducibility,
   not anything else.
3. **Add a third player and don't `STAND`/`HIT` them before calling
   `DEALER`.** Confirm you get the same
   `RoundNotReadyException` naming that specific player.
4. **Trace a soft-ace hand by hand.** Add `HANDS` calls after each `HIT`
   on a hand that contains an Ace, and watch `getTotal()`'s downgrade
   logic in action — the total should never silently exceed 21 as long as
   an ace is still available to convert from 11 to 1.
5. **Look for what happens if the dealer busts.** With a different seed
   (see #2), find a deal where the dealer's `playDealerTurn()` loop draws
   past 21. Confirm `outcome()`'s very first two checks
   (`playerStatus == BUSTED` then `dealerStatus == BUSTED`) correctly
   give every still-active/standing player a `"WIN"`.
