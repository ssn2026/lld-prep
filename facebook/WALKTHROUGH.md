# Facebook — Simple Walkthrough

This file explains the whole codebase in plain language, in the order you
should read it. No prior context assumed.

---

## 1. The story in one paragraph

Register some users. One user sends another a friend request, which sits
`PENDING` until the recipient either accepts it (both become friends) or
rejects it. Either user can post something. A user's news feed is built
by looking at every post authored by *that user themselves* or by anyone
they're currently friends with, newest first. Two people who were never
friends never see each other's posts. That's the whole system — a
deliberately small slice of "Facebook": users, friend requests with a
real lifecycle, posts, and a feed built from friendship.

---

## 2. The one door you're allowed to knock on

`src/services/FacebookService.java` is the **only** class anything
outside the package is meant to call.

| Method | What it does |
|---|---|
| `registerUser(name)` | Create a user, returns a generated id like `"u1"` |
| `sendFriendRequest(fromUserId, toUserId)` | Create a `PENDING` request, returns its id like `"r1"` |
| `acceptFriendRequest(requestId)` | `PENDING` -> `ACCEPTED`, and both users become friends |
| `rejectFriendRequest(requestId)` | `PENDING` -> `REJECTED` |
| `createPost(userId, content)` | Store a post, returns its id like `"p1"` |
| `getNewsFeed(userId, rankingStrategy)` | Every post by that user + their friends, ordered however you ask |
| `addListener(listener)` | Get notified of new posts and accepted requests |

---

## 3. Read the code in this order

### Step 1 — the plain data (`src/model/`)

- **`User.java`** — just an id and a name.
- **`Post.java`** — id, `authorId`, `content`, and a `sequence` int used
  purely for ordering (Step 3) — not a real timestamp, just a monotonic
  counter assigned when the post is created.
- **`FriendRequestStatus.java`** — enum: `PENDING`, `ACCEPTED`, `REJECTED`.
  Just the label; the logic for which transitions are legal lives in
  `state/` (Step 2).
- **`FriendRequest.java`** — the one model class with real behavior. Look
  at its field:

  ```java
  private FriendRequestState state = PendingState.INSTANCE;
  ```

  Every `FriendRequest` is born `PENDING`, and — just like `todo-list/`'s
  `Task` — its `accept()`/`reject()` methods don't contain any logic
  themselves; they delegate to whatever `FriendRequestState` is currently
  held:

  ```java
  public void accept() { state = state.accept(this); }
  public void reject() { state = state.reject(this); }
  ```

### Step 2 — one request's own lifecycle (`src/state/`)

This is the **State** pattern, and its doc comment (on
`FriendRequestState.java`) makes the design choice explicit: it's held
**per-instance**, on the `FriendRequest` itself — the same shape as
`todo-list/`'s per-`Task` `TaskState`, because there can be many friend
requests in flight at once, each independently `PENDING`, `ACCEPTED`, or
`REJECTED` — not the ATM/CrickInfo style of one shared state on the
service (there's no single "the" friend request for the whole system).

```java
public interface FriendRequestState {
    FriendRequestStatus getStatus();
    default FriendRequestState accept(FriendRequest request) {
        throw new InvalidFriendRequestTransitionException(getStatus(), "accept");
    }
    default FriendRequestState reject(FriendRequest request) {
        throw new InvalidFriendRequestTransitionException(getStatus(), "reject");
    }
}
```

Same trick as every other State-pattern problem in this repo: both
methods default to throwing. Only **`PendingState`** overrides anything:

```java
public FriendRequestState accept(FriendRequest request) { return AcceptedState.INSTANCE; }
public FriendRequestState reject(FriendRequest request) { return RejectedState.INSTANCE; }
```

`AcceptedState` and `RejectedState` both override nothing beyond
`getStatus()` — both are equally terminal. Once a request has been
decided, calling `accept()` or `reject()` on it again — from either
side — always throws `InvalidFriendRequestTransitionException`.

### Step 3 — how a feed gets ordered (`src/strategy/`)

```java
public interface FeedRankingStrategy {
    List<Post> rank(List<Post> posts);
}
```

One implementation, `ChronologicalFeedStrategy`:

```java
public List<Post> rank(List<Post> posts) {
    return posts.stream().sorted(Comparator.comparingInt(Post::getSequence).reversed()).toList();
}
```

Sorts by `sequence`, reversed, so the most-recently-created post comes
first. This is the **Strategy** pattern: *ordering* is completely
separate from *assembling* the list of candidate posts in the first
place (that part happens in `getNewsFeed()`, Step 5) — a different
ranking (say, most-liked-first, if this design grew a "likes" feature)
would be a new class implementing the same one method.

### Step 4 — who gets told when something happens (`src/observer/`)

```java
public interface FeedListener {
    void onNewPost(String authorId, String postId, String content);
    void onFriendRequestAccepted(String fromUserId, String toUserId);
}
```

`ConsoleFeedListener` is the one shipped implementation — two `println`s.
This is the **Observer** pattern, and it's worth noticing what it's
*not* used for here: sending a notification to every one of the author's
friends whenever they post. `getNewsFeed()` (Step 5) is a **pull**
model — nobody gets pushed anything; a feed is computed fresh, on
demand, by querying "every post by this person or their friends" at the
moment you ask. The Observer here is scoped narrowly to "something
happened" (a post was made, a request was accepted) — not to personalized
delivery.

### Step 5 — looking things up (`src/repository/`)

Four small repositories, three of the same familiar shape (a `Map` plus
save/find, throwing a specific not-found exception on a miss):

- **`UserRepository.java`** — keyed by user id; `findById` throws
  `UserNotFoundException`.
- **`FriendRequestRepository.java`** — keyed by request id; `findById`
  throws `FriendRequestNotFoundException`. It has one more method worth
  reading closely:

  ```java
  public boolean hasPendingBetween(String userA, String userB) {
      return requestsById.values().stream().anyMatch(r ->
              r.getStatus() == FriendRequestStatus.PENDING
                      && ((r.getFromUserId().equals(userA) && r.getToUserId().equals(userB))
                      || (r.getFromUserId().equals(userB) && r.getToUserId().equals(userA))));
  }
  ```

  This checks **both directions** — a pending request from A to B counts
  the same as one from B to A. It also only counts requests that are
  still `PENDING` — an already-`ACCEPTED` or `REJECTED` request between
  the same two people doesn't block a fresh one (though see Step 6:
  accepted pairs are blocked by a *different* check first, before this
  one is even reached).

- **`FriendshipRepository.java`** — a map from `userId` to a `Set<String>`
  of that user's friends' ids. `addFriendship(a, b)` writes **both
  directions at once** (`a`'s set gets `b` added, and `b`'s set gets `a`
  added) — friendship is inherently mutual, so there's no such thing as
  "A is B's friend but B isn't A's."
- **`PostRepository.java`** — just a flat `List<Post>`, with
  `findByAuthors(authorIds)` filtering it down to posts whose author is in
  a given set of ids — this is exactly what powers a feed (Step 5).

### Step 6 — errors (`src/exceptions/`)

Six small exceptions, each doing one job:

- `UserNotFoundException` / `FriendRequestNotFoundException` — unknown
  ids.
- `SelfFriendRequestException` — a user tried to friend-request
  themselves.
- `AlreadyFriendsException` — the two users are already friends.
- `DuplicatePendingRequestException` — there's already a pending request
  between the two (in either direction).
- `InvalidFriendRequestTransitionException` — thrown by
  `FriendRequestState`'s throwing defaults (Step 2), for accepting or
  rejecting a request that's already been decided.

### Step 7 — the orchestrator (`src/services/FacebookService.java`)

`sendFriendRequest()` is worth reading closely for its **order** of
checks:

```java
public String sendFriendRequest(String fromUserId, String toUserId) {
    userRepository.findById(fromUserId);
    userRepository.findById(toUserId);
    if (fromUserId.equals(toUserId)) {
        throw new SelfFriendRequestException(fromUserId);
    }
    if (friendshipRepository.areFriends(fromUserId, toUserId)) {
        throw new AlreadyFriendsException(fromUserId, toUserId);
    }
    if (requestRepository.hasPendingBetween(fromUserId, toUserId)) {
        throw new DuplicatePendingRequestException(fromUserId, toUserId);
    }
    String requestId = "r" + requestSeq.incrementAndGet();
    FriendRequest request = new FriendRequest(requestId, fromUserId, toUserId);
    requestRepository.save(request);
    return requestId;
}
```

Both users must actually exist first (two separate `findById` calls —
either can throw `UserNotFoundException`). Then: not yourself, not
someone you're already friends with, not someone you already have a
pending request with. Only once every check passes does a `FriendRequest`
object actually get created — no partial state, no request ever gets
saved if any earlier check would have rejected it.

`acceptFriendRequest()` shows the same "state transition, then its real
consequences" ordering seen elsewhere in this repo:

```java
public void acceptFriendRequest(String requestId) {
    FriendRequest request = requestRepository.findById(requestId);
    request.accept();
    friendshipRepository.addFriendship(request.getFromUserId(), request.getToUserId());
    notifyFriendRequestAccepted(request.getFromUserId(), request.getToUserId());
}
```

`request.accept()` runs **first** — if the request isn't currently
`PENDING`, this throws immediately, and neither the friendship nor the
notification ever happens. Only once the state transition itself
succeeds do the real-world consequences (recording the friendship,
telling listeners) follow.

`getNewsFeed()` is the last piece:

```java
public List<Post> getNewsFeed(String userId, FeedRankingStrategy rankingStrategy) {
    userRepository.findById(userId);
    Set<String> authorIds = new HashSet<>(friendshipRepository.getFriendsOf(userId));
    authorIds.add(userId);
    return rankingStrategy.rank(postRepository.findByAuthors(authorIds));
}
```

Note `authorIds.add(userId)` — a user's own posts are always part of
their own feed, in addition to every friend's. The whole method is only
three real steps: gather the set of relevant authors (friends + self),
fetch every post by any of them, and hand that list to whichever
`FeedRankingStrategy` was passed in to decide the order.

### Step 8 — the runner (`src/Main.java`)

A test harness reading `test/input/scenario.txt`, driving
`FacebookService`, and writing a transcript to `test/output/output.txt`.
It registers two listeners — `ConsoleFeedListener` and a small private
`TranscriptFeedListener` defined inside `Main.java` itself — so listener
events land in the saved transcript file, not just the terminal.

---

## 4. Picture of one full flow: accept a request, then check both feeds

```
Main.java (reads "ACCEPT r1", a PENDING request from u1 to u2)
   |
   v
FacebookService.acceptFriendRequest("r1")
   |  request = requestRepository.findById("r1")
   |  request.accept()
   |       state.accept(this)   <- PendingState.accept() returns AcceptedState.INSTANCE
   |       state = AcceptedState.INSTANCE
   |  friendshipRepository.addFriendship("u1", "u2")
   |       u1's friend set gains "u2"; u2's friend set gains "u1"  (both directions)
   |  notifyFriendRequestAccepted("u1", "u2")   <- every FeedListener.onFriendRequestAccepted("u1", "u2")
   v
Main.java prints: "OK r1 accepted"


... later ...

Main.java (reads "FEED u1")
   |
   v
FacebookService.getNewsFeed("u1", new ChronologicalFeedStrategy())
   |  userRepository.findById("u1")            -> exists
   |  authorIds = friendshipRepository.getFriendsOf("u1")   -> {"u2"}
   |  authorIds.add("u1")                       -> {"u1", "u2"}
   |  postRepository.findByAuthors({"u1","u2"}) -> [post by u1, post by u2]
   |  rankingStrategy.rank([...])                -> sorted by sequence, descending
   v
returns [most recent post first, ...]
```

---

## 5. Reading the actual captured run (`test/output/output.txt`)

```
> USER Alice
OK u1 = Alice
> USER Bob
OK u2 = Bob
> USER Carol
OK u3 = Carol
```

Three users, ids assigned in registration order — `u1`, `u2`, `u3`.

```
> REQUEST u1,u2
OK r1 (u1 -> u2)
> REQUEST u1,u3
OK r2 (u1 -> u3)
> REQUEST u2,u1
ERROR DuplicatePendingRequestException: A pending friend request already exists between u2 and u1
```

`r1` (u1 → u2) is still `PENDING` when `u2,u1` is attempted — and per
Step 5, `hasPendingBetween` checks both directions, so a *reversed*
request between the same pair is correctly caught too, not just an exact
duplicate.

```
> ACCEPT r1
  [listener] u1 and u2 are now friends
OK r1 accepted
> REJECT r2
OK r2 rejected
```

`r1` (u1↔u2) becomes a real friendship. `r2` (u1↔u3) is rejected instead
— u1 and u3 are **not** friends after this.

```
> POST u1 Hello from Alice
  [listener] u1 posted p1: Hello from Alice
OK p1 by u1
> POST u2 Hello from Bob
  [listener] u2 posted p2: Hello from Bob
OK p2 by u2
> POST u3 Hello from Carol
  [listener] u3 posted p3: Hello from Carol
OK p3 by u3
```

Every user posts once. Post ids `p1`, `p2`, `p3` in creation order, each
carrying its own `sequence` (1, 2, 3 respectively, though that's internal
and not printed directly here).

```
> FEED u1
FEED u1
  p2 [u2] Hello from Bob
  p1 [u1] Hello from Alice
```

u1's feed shows exactly two posts: their own (`p1`) and their one
friend's (`p2`, from u2). **Not** `p3` (Carol's) — u1 and u3 aren't
friends, since that request was rejected. And notice the order: `p2`
(sequence 2) before `p1` (sequence 1) — newest first, exactly matching
`ChronologicalFeedStrategy`'s reversed sort.

```
> FEED u3
FEED u3
  p3 [u3] Hello from Carol
```

u3 has no friends at all (their one request was rejected), so their feed
contains only their own post.

```
> REQUEST u1,u2
ERROR AlreadyFriendsException: u1 and u2 are already friends
```

u1 and u2 are already friends from `r1`'s acceptance — a fresh request
between them is correctly rejected before it ever reaches the
duplicate-pending check.

```
> REQUEST u1,u1
ERROR SelfFriendRequestException: User u1 cannot send a friend request to themselves
```

The very first check in `sendFriendRequest()` (after both users are
confirmed to exist) — `fromUserId.equals(toUserId)`.

```
> ACCEPT r1
ERROR InvalidFriendRequestTransitionException: Cannot accept a friend request that is ACCEPTED
> REJECT r2
ERROR InvalidFriendRequestTransitionException: Cannot reject a friend request that is REJECTED
```

Both requests were already decided earlier in the script. `AcceptedState`
and `RejectedState` both override nothing beyond `getStatus()`, so
calling either transition method on either one falls through to
`FriendRequestState`'s throwing default.

```
> ACCEPT r99
ERROR FriendRequestNotFoundException: No friend request found with id: r99
> POST u99 This should fail
ERROR UserNotFoundException: No user found with id: u99
> FEED u99
ERROR UserNotFoundException: No user found with id: u99
```

Three different not-found paths — an unknown request id, and an unknown
user id attempted from two different entry points (`createPost` and
`getNewsFeed`), both correctly guarded by their own `userRepository.findById(...)`
call before anything else runs.

---

## 6. Things to try / test yourself

Edit `test/input/scenario.txt` and re-run:
```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

1. **Send a request, then send the reverse before it's decided.** `REQUEST
   u1,u2` then immediately `REQUEST u2,u1` — confirm the second one fails
   with `DuplicatePendingRequestException`, proving the "both directions"
   check in `hasPendingBetween()`.
2. **Build a friend-of-a-friend chain and confirm it's NOT in the feed.**
   Make u1 and u2 friends, and u2 and u3 friends (but u1 and u3 are
   *not*). Confirm u1's feed shows u2's posts but not u3's — friendship
   in this design is direct only, never transitive.
3. **Accept a request, then check the OTHER user's friend list too.**
   After `ACCEPT r1` (u1→u2), send a *new* request from u2 to some third
   user and confirm `hasPendingBetween`/`areFriends` for the u1/u2 pair
   correctly still shows them as friends — proving `addFriendship`'s
   both-directions write actually took effect for both users, not just
   the one who sent the original request.
4. **Reject a request, then send a fresh one between the same two
   people.** After `REJECT r2` (u1→u3), try `REQUEST u1,u3` again —
   confirm this **succeeds** (a new request, a new id), since
   `hasPendingBetween` only counts requests that are still `PENDING`, and
   `areFriends` is `false` for a rejected pair.
5. **Post several times from the same user and confirm the feed orders
   them correctly.** Three or four `POST u1 ...` calls in a row, then
   `FEED u1` — confirm the most recent one appears first, matching each
   post's `sequence` value in reverse.
