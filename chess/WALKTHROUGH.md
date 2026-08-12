# Chess — Simple Walkthrough

This file explains the whole codebase in plain language, in the order you
should read it. No prior context assumed.

---

## 1. The story in one paragraph

You're building a two-player chess engine: a standard 8x8 board, the six
piece types, and the rules for how each one is allowed to move. Two players
take turns submitting moves; the engine has to reject anything illegal (not
your turn, not how that piece moves, or a move that would leave your own
king exposed to capture) and, after every accepted move, has to figure out
the game's new status from scratch — is the other player now in check? Do
they have any legal move left at all? Those two questions together decide
whether the game is just "in progress," "check," "checkmate" (game over,
someone won), or "stalemate" (game over, a draw).

---

## 2. The one door you're allowed to knock on

`src/services/ChessGameService.java` is the **only** class anything outside
the package is meant to call. Everything else (`model`, `factory`, `state`,
`observer`, `exceptions`) is a helper it uses internally.

| Method | What it does |
|---|---|
| `new ChessGameService(whitePlayer, blackPlayer)` | Sets up the standard starting position |
| `addObserver(observer)` | Register something to be notified after each move |
| `makeMove(fromAlgebraic, toAlgebraic)` | Attempt a move (defaults pawn promotion to Queen) |
| `makeMove(fromAlgebraic, toAlgebraic, promotionChoice)` | Same, with an explicit promotion piece |
| `renderBoard()` | Text rendering of the current board |
| `getStatus()` | Current `GameStatus` (`IN_PROGRESS`/`CHECK`/`CHECKMATE`/`STALEMATE`) |
| `getCurrentTurn()` | Whose turn it is (`Color.WHITE`/`Color.BLACK`) |
| `getMoveHistory()` | A copy of every move played so far |
| `getWhitePlayer()` / `getBlackPlayer()` | The two `Player`s |

---

## 3. Read the code in this order

### Step 1 — the small, non-piece building blocks (`src/model/`)

- **`Color.java`** — enum `WHITE`/`BLACK` with one helper, `opposite()`.
  Used constantly (whose turn is next, which side is attacking).
- **`PieceType.java`** — enum `PAWN`/`KNIGHT`/`BISHOP`/`ROOK`/`QUEEN`/`KING`.
- **`GameStatus.java`** — enum `IN_PROGRESS`/`CHECK`/`CHECKMATE`/`STALEMATE`.
  Just the label; the logic that decides which one applies lives in
  `state/` (Step 4).
- **`Position.java`** — a `(row, col)` square, with `row 0` = rank 1
  (White's back rank) and `col 0` = file `a`, per the class comment.
  `Position.of("e2")` parses standard algebraic notation into a `Position`,
  throwing `IllegalArgumentException` for anything malformed or off-board.
  `equals()`/`hashCode()` are defined by row/col, so two separately
  constructed `Position` objects for the same square are treated as equal
  — this matters because `Piece.getPossibleMoves()` returns a `List` of
  brand-new `Position` objects each call, and callers need
  `list.contains(dest)` to work by value, not by object identity.
- **`Player.java`** — an id, a name, and a `Color`. Plain data.
- **`Move.java`** — an immutable record of one completed move: from, to,
  what piece moved, who moved it, what (if anything) was captured, and
  what it promoted to (if it was a promoting pawn move). Its `toString()`
  is what produces the `"WHITE PAWN f2-f3"` style text you see in the
  transcript.

### Step 2 — the piece hierarchy (`src/model/Piece.java` and subclasses)

`Piece.java` is an **abstract class** every concrete piece extends. It
holds the shared state every piece needs (`color`, `position`, `hasMoved`)
and declares two abstract methods every subclass must implement:
`getType()` and `getPossibleMoves(Board)`. This is plain **polymorphism**
— each subclass knows its own movement geometry, and any code holding a
`Piece` reference can call `getPossibleMoves(board)` without caring which
concrete type it actually is. The README is explicit that this isn't
counted as one of the project's headline three patterns (Factory/State/
Observer, covered next) — it's just normal object-oriented design, doing
the job Strategy did in some of the other LLD problems in this repo.

A detail worth calling out: **`getPossibleMoves()` returns *pseudo-legal*
moves only** — moves that respect board boundaries and don't land on your
own piece, but that are "blind to whether the move would leave your own
king in check" (straight from the class comment on `Piece`). Filtering
those down to fully legal moves is deliberately not this class's job — it
belongs to `Board`, because checking "would this leave my king in check"
requires simulating the whole board, not just one piece's geometry (see
Step 3).

- **`Pawn.java`** — the most irregular piece: one square forward (two if
  it hasn't moved yet, via the `hasMoved` flag), and diagonal moves only
  when there's an enemy piece to capture there (a pawn never moves
  diagonally onto an empty square in this design — no en passant, flagged
  in the README's "Known gaps").
- **`Knight.java`** / **`King.java`** — both use a fixed array of `(row,
  col)` offsets (`OFFSETS`) and just check each one is on the board and
  not occupied by your own piece. Structurally almost identical to each
  other; the only difference is the offset table (knight's L-shapes vs.
  king's 8 adjacent squares) and that `King` has a comment flagging "no
  castling in this design's scope."
- **`Rook.java`** / **`Bishop.java`** / **`Queen.java`** — all three
  delegate to a shared helper, `SlidingPieceSupport.slide(board, position,
  color, directions)`, passing only their own direction set (rook:
  horizontal/vertical; bishop: diagonal; queen: both combined — literally
  the union of rook's and bishop's `DIRECTIONS` arrays). `slide()` walks
  outward in each direction until it hits the edge of the board, an
  enemy piece (included as a legal capture, then stops), or a friendly
  piece (excluded, then stops). Pulling this into one shared, private
  helper class (`SlidingPieceSupport`, package-private and not itself a
  `Piece`) avoids writing the same ray-casting loop three times.
- Every piece also implements `copy()` — a defensive deep copy of just
  itself. This exists purely to support `Board.copy()` (next), which
  needs to simulate a hypothetical move without mutating the real board.

### Step 3 — how pieces get created without callers `new`-ing subclasses (`src/factory/PieceFactory.java`)

This is the **Factory** pattern: one static method,
`createPiece(PieceType, Color, Position)`, with a `switch` over
`PieceType` that returns the matching concrete subclass. It's used in
exactly two places: `ChessGameService.setupBoard()` (laying out all 32
starting pieces) and pawn promotion
(`ChessGameService.applyPromotionIfNeeded()`). Centralizing construction
here means neither of those call sites — nor any future one — needs to
know or care which `Piece` subclasses exist; they just ask for a
`PieceType` and get the right object back.

### Step 4 — the board, and where "is this move actually legal" is decided (`src/model/Board.java`)

`Board` owns the 8x8 grid (`Piece[][] grid`) and, per its class comment,
"every piece of board-scoped logic that needs a whole-board view." This is
worth reading carefully because it's the trickiest part of the whole
design:

- `getPiece(pos)` / `placePiece(piece, pos)` / `removePiece(pos)` — direct
  grid access.
- `getPiecesByColor(color)` — scans the whole grid for one side's pieces.
  Used by `isSquareAttacked` and `hasAnyLegalMove` below.
- `findKing(color)` — finds that color's king's `Position` (throws
  `IllegalStateException` if somehow missing — should never happen in
  normal play).
- `isSquareAttacked(target, byColor)` — true if *any* piece of `byColor`
  has `target` in its `getPossibleMoves(this)`. This is how "is the king
  in check" gets answered: check whether the king's own square is attacked
  by the opposing color.
- **`isMoveLegal(piece, dest)`** — the heart of the legality check. It
  calls `copy()` to get a full, independent clone of the board, finds the
  moving piece's clone on that copy, relocates it to `dest` (removing
  whatever was there, simulating a capture), finds where that color's king
  ends up on the *simulated* board, and asks `isSquareAttacked` whether
  the opposing color now attacks that king square. If yes, the move would
  leave (or put) your own king in check, so it's illegal — even if it was
  a perfectly valid pseudo-legal move geometrically.
- **`hasAnyLegalMove(color)`** — for every one of that color's pieces, for
  every one of its pseudo-legal destinations, ask `isMoveLegal`. Returns
  true the moment it finds even one fully-legal move anywhere on the
  board. This is expensive (it's trying every piece's every move,
  each requiring its own board copy+simulation) but simple and correct,
  and this codebase is optimizing for "an LLD interview design," not
  competitive-engine performance.
- **`copy()`** — builds a brand-new `Board` and deep-copies every occupied
  square via each `Piece.copy()`. This is what makes `isMoveLegal` and
  `hasAnyLegalMove` safe to call freely — they never mutate the real,
  live board, only throwaway clones.

The README calls this out directly: because both `ChessGameService.makeMove`
(validating one submitted move) and the `state` package's checkmate/
stalemate detection (Step 5, which needs "does this color have *any* legal
move at all") both call into `isMoveLegal`/`hasAnyLegalMove`, the actual
simulate-and-check logic exists in exactly one place — neither caller
reimplements its own copy of it.

### Step 5 — what state is the game in? (`src/state/`)

This is the **State** pattern, and it looks a lot like the ATM problem's
State usage in this same repo (holding a single state field directly on
the service, not on a model object) — the README even calls that
similarity out.

- **`GameState.java`** — the interface: `getStatus()` and `allowsMove()`.
  It also has a `static` method, `evaluate(board, colorToMove)`, that is
  the **one place** the actual check/checkmate/stalemate math happens:
  ```java
  boolean inCheck = board.isSquareAttacked(board.findKing(colorToMove), colorToMove.opposite());
  boolean hasLegalMove = board.hasAnyLegalMove(colorToMove);
  if (!hasLegalMove) {
      return inCheck ? CheckmateState.INSTANCE : StalemateState.INSTANCE;
  }
  return inCheck ? CheckState.INSTANCE : InProgressState.INSTANCE;
  ```
  In plain words: no legal moves left, and you're in check right now ->
  checkmate (you lose). No legal moves left, but you're *not* in check ->
  stalemate (a draw — you're just stuck, not attacked). Legal moves
  remain, and you're in check -> check (still your problem to solve, but
  the game continues). Legal moves remain, not in check -> normal,
  in-progress game.
- **`InProgressState.java`** / **`CheckState.java`** — both are tiny
  singletons whose `allowsMove()` returns `true` — the game keeps going
  from either.
- **`CheckmateState.java`** / **`StalemateState.java`** — both singletons
  whose `allowsMove()` returns `false` — these are the two terminal
  states. Once here, `ChessGameService.makeMove()`'s very first check
  (`!currentState.allowsMove()`) rejects every further move attempt with a
  `GameOverException`, without even looking at the board.

Every concrete state is a `public static final INSTANCE` singleton with a
private constructor, exactly like the ATM problem's states — there's only
ever one game in progress at a time here, so there's no reason to allocate
new state objects.

### Step 6 — who gets told what happened (`src/observer/`)

This is the **Observer** pattern.

- **`GameObserver.java`** — interface with three callbacks: `onMove(move)`
  (called after every accepted move), `onCheck(colorInCheck)` (called only
  when the new state is `CHECK`), and `onGameOver(finalStatus, winner)`
  (called only for `CHECKMATE`/`STALEMATE`, with `winner` set to the
  color that delivered checkmate, or `null` for a stalemate draw).
- **`ConsoleGameObserver.java`** — the one implementation shipped with the
  design; each callback just `System.out.println`s a human-readable line
  (e.g. `"  [event] CHECKMATE - WHITE wins"`).

`ChessGameService` doesn't know or care who's listening — it just holds a
`List<GameObserver>` and loops over it, calling `onMove` on every accepted
move, and *additionally* calling `onCheck` or `onGameOver` depending on
what `currentState.getStatus()` came back as after the move. Read
`notifyObservers()` in `ChessGameService` to see this decided.

### Step 7 — errors (`src/exceptions/`)

Two small `RuntimeException` subclasses:

- `InvalidMoveException` — the submitted move itself is illegal (no piece
  there, wrong player's piece, not a pseudo-legal destination for that
  piece, or would leave your own king in check).
- `GameOverException` — a move was attempted after the game already ended
  in checkmate or stalemate.

`Main.java` also separately catches `IllegalArgumentException`, which is
what `Position.of()` throws for a malformed algebraic square (e.g. `"z9"`)
— that one isn't a custom exception, it's a standard Java one, thrown
before the move logic even gets a real `Position` to work with.

### Step 8 — the orchestrator (`src/services/ChessGameService.java`)

Now that you've seen every collaborator, `makeMove()` is worth reading
top to bottom — it's the single most important method in the codebase.
In order:

1. `!currentState.allowsMove()` -> throw `GameOverException` immediately if
   the game already ended.
2. Parse both algebraic squares into `Position`s.
3. Look up the piece at `from` -> throw `InvalidMoveException` if empty.
4. Check `piece.getColor() != currentTurn` -> throw if it's the wrong
   player's piece.
5. Check `!piece.getPossibleMoves(board).contains(to)` -> throw if `to`
   isn't even a pseudo-legal destination for that piece.
6. Check `!board.isMoveLegal(piece, to)` -> throw if the move would leave
   the mover's own king in check.
7. **Only now**, with every check passed: capture whatever's at `to` (if
   anything), relocate the piece, mark it `hasMoved`, and handle
   promotion (`applyPromotionIfNeeded` — a pawn reaching row 0 or row 7
   gets replaced via `PieceFactory` with the chosen piece type, defaulting
   to `QUEEN` if none was specified).
8. Build a `Move` record, append it to `moveHistory`, flip `currentTurn`
   to the opponent.
9. Recompute the game's status from scratch: `currentState =
   GameState.evaluate(board, currentTurn)` — note this is evaluated for
   the color about to move *next*, i.e. "is my opponent now in check or
   stuck?"
10. `notifyObservers(move)`.

Everything from step 3 through step 6 is ordered specifically so cheap,
obvious checks (is there even a piece, is it your turn) run before the
expensive one (`isMoveLegal`, which clones the whole board) — a small but
deliberate efficiency choice.

---

## 4. Order of operations — tracing the real captured game (Fool's Mate)

The test scenario plays out the fastest possible real checkmate in chess —
"Fool's Mate" — after a couple of illegal-move edge cases. Here's the
sequence of real method calls behind the final, decisive move.

```
Main.java: "MOVE d8 h4"   (Black's queen delivers checkmate)
   |
   v
ChessGameService.makeMove("d8", "h4", null)
   | currentState.allowsMove() -- CheckState/InProgressState both true at this point
   | from = Position.of("d8"), to = Position.of("h4")
   | piece = board.getPiece(d8)   -- the Black Queen
   | piece.getColor() == currentTurn (BLACK)?  yes
   | piece.getPossibleMoves(board).contains(h4)?
   |     Queen delegates to SlidingPieceSupport.slide() -- the diagonal
   |     d8-e7-f6-g5-h4 is completely open (White's f-pawn and g-pawn
   |     moves earlier vacated the squares that would have blocked it)
   |     -> yes, h4 is reachable
   | board.isMoveLegal(piece, h4)?
   |     simulate: Black queen moves to h4 on a cloned board
   |     Black's own king isn't affected -- legal
   | captured = board.getPiece(h4) -- null, nothing there
   | board.removePiece(d8); piece.setPosition(h4); piece.setHasMoved(true)
   | board.placePiece(piece, h4)
   | applyPromotionIfNeeded -- not a pawn, no-op
   | move = new Move(d8, h4, QUEEN, BLACK, null, null)
   | moveHistory.add(move)
   | currentTurn = WHITE   (flips to the side about to move next)
   | currentState = GameState.evaluate(board, WHITE)
   |     inCheck = board.isSquareAttacked(White king's square e1, BLACK)
   |         -- the Black queen on h4 now attacks e1 along the open
   |            h4-g3-f2-e1 diagonal -> true
   |     hasLegalMove = board.hasAnyLegalMove(WHITE)
   |         -- every White piece's every pseudo-legal move is simulated;
   |            none gets the king out of check (blocking the diagonal,
   |            capturing the queen, or moving the king to safety are all
   |            unavailable given the current position) -> false
   |     !hasLegalMove && inCheck -> CheckmateState.INSTANCE
   | notifyObservers(move)
   |     every observer.onMove(move)
   |     status == CHECKMATE -> every observer.onGameOver(CHECKMATE, winner=BLACK)
   v
returns the Move object
Main.java prints "OK BLACK QUEEN d8-h4 | turn now WHITE, status CHECKMATE"
```

And the very next attempted move proves the terminal state actually
freezes the game:

```
Main.java: "MOVE a2 a3"
   v
ChessGameService.makeMove("a2", "a3", null)
   | currentState.allowsMove()  -- CheckmateState.allowsMove() returns false
   | throws GameOverException("Game is over (CHECKMATE); no more moves allowed")
```

Notice this throws **before** even parsing `a2`/`a3` into `Position`s or
looking at the board at all — the state guard is the very first line of
`makeMove()`.

---

## 5. Reading the actual captured run (`test/output/output.txt`)

The two illegal-move edge cases at the very start:

```
> MOVE e1 e2
ERROR InvalidMoveException: KING cannot move from e1 to e2
```

The White king starts on e1; e2 is occupied by White's own pawn.
`King.getPossibleMoves()` never even lists e2 as a candidate (a friendly
piece blocks it), so `piece.getPossibleMoves(board).contains(to)` is false
and `makeMove()` throws before ever reaching `isMoveLegal`.

```
> MOVE e7 e5
ERROR InvalidMoveException: It is WHITE's turn, not BLACK
```

This is Black's pawn, submitted before White has made a single move — the
turn check (`piece.getColor() != currentTurn`) rejects it, and note the
message correctly names `currentTurn` (WHITE) as whose turn it actually is,
not the piece's own color.

The three moves that set up the mate:

```
> MOVE f2 f3
OK WHITE PAWN f2-f3 | turn now BLACK, status IN_PROGRESS
> MOVE e7 e5
OK BLACK PAWN e7-e5 | turn now WHITE, status IN_PROGRESS
> MOVE g2 g4
OK WHITE PAWN g2-g4 | turn now BLACK, status IN_PROGRESS
```

The rendered board right after these three moves:

```
> BOARD
8 r n b q k b n r 
7 p p p p . p p p 
6 . . . . . . . . 
5 . . . . p . . . 
4 . . . . . . P . 
3 . . . . . P . . 
2 P P P P P . . P 
1 R N B Q K B N R 
  a b c d e f g h
```

You can read the two White pawn moves directly off the grid: rank 2 (row
`2`) is missing its `f` and `g` pawns (blank there), while rank 3 and rank
4 each show a lone `P` in the `f` and `g` columns respectively — exactly
matching `f2-f3` and `g2-g4`. This weakening of White's kingside pawn
shield is precisely what opens the long diagonal the Black queen uses next.

The mating move and the final board:

```
> MOVE d8 h4
OK BLACK QUEEN d8-h4 | turn now WHITE, status CHECKMATE
> BOARD
8 r n b . k b n r 
7 p p p p . p p p 
6 . . . . . . . . 
5 . . . . p . . . 
4 . . . . . . P q 
3 . . . . . P . . 
2 P P P P P . . P 
1 R N B Q K B N R 
  a b c d e f g h
```

Rank 8's `d`-file is now blank (the queen left d8) and rank 4 shows a
lowercase `q` next to White's `P` on the `g` file — the Black queen (lower
case = Black, per `Piece.getSymbol()`) landed on h4, directly threatening
the White king down the diagonal.

```
> STATUS
STATUS turn=WHITE status=CHECKMATE
> MOVE a2 a3
ERROR GameOverException: Game is over (CHECKMATE); no more moves allowed
```

Confirms the game is frozen — `CheckmateState.allowsMove()` returning
`false` blocks even a completely unrelated, otherwise-legal pawn push.

**One thing worth noticing while reading this file:** `ConsoleGameObserver`
prints its own `"[event] ..."` lines directly to `System.out` inside
`onMove`/`onCheck`/`onGameOver`, completely separately from `Main.java`'s
`log()` helper. Since `test/output/output.txt` is built only from what
`log()` explicitly appends, none of the observer's `[event]` lines actually
appear in the captured transcript file — they only show up if you run the
program yourself and watch the live console. If you want to see the
Observer pattern's output land in the transcript too, that would take a
small change to how `Main.java` wires up the observer (e.g. an observer
that appends to `output` instead of printing).

---

## 6. Things to try / test yourself

You can edit `test/input/scenario.txt` and re-run with:
```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

1. **Play out a real stalemate instead of a checkmate.** Stalemates are
   notoriously fiddly to set up by hand near the start of a game — an
   easier way to explore this is to read `GameState.evaluate()` again and
   trace by hand what board state would make `hasAnyLegalMove(color)`
   false while `inCheck` is also false, then see if you can construct a
   short move sequence (may take some experimentation) that reaches it.
2. **Try to move into check on purpose.** After White's queen is
   developed, try moving a piece that's the only thing blocking an
   attack on White's own king so the king would be exposed. Confirm you
   get `InvalidMoveException` with the "would leave WHITE's king in
   check" message, and that it comes from `board.isMoveLegal` returning
   false, not from a piece's own `getPossibleMoves`.
3. **Promote a pawn to something other than a Queen.** Get a pawn to the
   back rank (this needs a longer script than the current Fool's Mate
   scenario) and issue `MOVE <from> <to> ROOK` (a 4th token — `Main.java`
   parses `parts[3]` as the optional `PieceType`). Confirm the promoted
   piece's symbol on the next `BOARD` render matches a rook, not a queen.
4. **Try an out-of-range square.** `MOVE e1 z9` or `MOVE i1 e2`. Expect
   `ERROR IllegalArgumentException: Invalid square: ...`, thrown by
   `Position.of()` before `makeMove()` gets anywhere near the board.
5. **Capture a piece and check the transcript.** Any move where `to`
   lands on an enemy piece — confirm the printed `Move.toString()` shows
   an `x` (capture) instead of `-`, and includes `(captures <TYPE>)`,
   proving `captured != null` was correctly threaded from `board.getPiece(to)`
   through to the `Move` object.
6. **Add a second `GameObserver`.** In `Main.java`, register a second
   `GameObserver` implementation alongside `ConsoleGameObserver` (similar
   to how the job-scheduler problem's `Main.java` registers two
   `JobListener`s). Confirm both get called on every move without either
   knowing the other exists — proof the Observer pattern here really does
   support multiple independent listeners.
