# Facebook

> **Mode: Learning** — this design (happy flow, class breakdown, pattern
> choices) was authored by Claude, not the user. See `CLAUDE.md` → "Mode:
> Learning" for what that implies about ownership of the decisions below.

A minimal social-network core, scoped down from "build Facebook" to its
essential mechanics: users, friend requests with a real accept/reject
lifecycle, posts, and a news feed built from your own posts plus your
friends'.

## Happy flow

1. `FacebookService.registerUser(name)` creates a `User`.
2. `sendFriendRequest(from, to)` validates both users exist, rejects
   self-requests, rejects requests between people who are already friends,
   and rejects a second pending request between the same pair — then
   creates a `FriendRequest` in `PendingState`.
3. `acceptFriendRequest(id)`/`rejectFriendRequest(id)` ask the request's
   own current state whether that move is legal; accepting also records
   the friendship (both directions) and notifies listeners.
4. `createPost(userId, content)` stores a `Post` and notifies listeners.
5. `getNewsFeed(userId, rankingStrategy)` pulls every post authored by that
   user or any of their friends and orders it however the caller wants.

## Design patterns used

- **State** — `state/FriendRequestState.java` (interface with throwing
  defaults) plus `PendingState`/`AcceptedState`/`RejectedState`
  singletons. Same shape as `todo-list/`'s per-`Task` `TaskState`: every
  `FriendRequest` holds its own state reference — many independent
  lifecycles, not one shared machine — so `FacebookService` never branches
  on `FriendRequestStatus` itself; it calls `request.accept()`/`reject()`
  and lets the request's current state decide what's legal.
- **Strategy** — `strategy/FeedRankingStrategy.java` with
  `ChronologicalFeedStrategy`. How a feed is *ordered* is fully decoupled
  from how it's *assembled* (self + friends' posts) — a different ordering
  (e.g. an engagement-based ranking) is a new class, not a branch inside
  `getNewsFeed()`.
- **Observer** — `observer/FeedListener.java` with `ConsoleFeedListener`.
  New posts and accepted friend requests are both reported the same way.
  Deliberately a *pull* model for the feed itself (`getNewsFeed()` queries
  on demand) rather than push-fan-out-to-every-friend-on-every-post, which
  keeps the Observer's job scoped to "something happened" notifications
  instead of personalized delivery.

## Structure

```
facebook/
  src/
    model/       User, Post, FriendRequest (owns its own State), FriendRequestStatus
    state/       FriendRequestState + Pending/Accepted/RejectedState
    strategy/    FeedRankingStrategy + ChronologicalFeedStrategy
    observer/    FeedListener + ConsoleFeedListener
    repository/  UserRepository, FriendRequestRepository, FriendshipRepository,
                 PostRepository (all in-memory)
    exceptions/  UserNotFoundException, FriendRequestNotFoundException,
                 SelfFriendRequestException, AlreadyFriendsException,
                 DuplicatePendingRequestException,
                 InvalidFriendRequestTransitionException
    services/    FacebookService (the only public entry point)
    Main.java    Reads a command script, drives the service, writes a transcript
  test/
    input/scenario.txt   3 users, 2 requests (one accepted, one rejected),
                          posts, feeds that correctly include/exclude based
                          on friendship, and every guard/error path
    output/output.txt    Captured run transcript, including every Observer
                          event (see Main.java's TranscriptFeedListener)
  diagrams/
    generate.py        Data-only script that builds facebook.drawio
    facebook.drawio    Class diagram + 1 sequence diagram (getNewsFeed())
  explainer/index.html   Interactive step-through: send/accept/reject friend
                          requests, create posts, and view any user's feed while
                          watching the real State transitions and feed-assembly
                          logic play out
```

## Running it

```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

Requires the VS Code "Extension Pack for Java" (Microsoft) for the
`.vscode/launch.json` run config to work — open the `facebook/` folder
itself as the workspace root, then use the "Run Main (scenario.txt)" config.

## Known gaps (flagged, not fixed)

- No persistence — everything is in-memory and lost on process exit.
- No unfriending, no comments/likes/reactions, no privacy settings — the
  scope is deliberately just users, friend requests, posts, and feeds.
- Friend requests are unidirectional but symmetric once accepted — there's
  no separate "follow" relationship distinct from mutual friendship.
- `getNewsFeed()` has no pagination — it always returns every matching
  post, which wouldn't scale to a real feed's volume.
- No blocking/unfriending path back from `AcceptedState` — once two users
  are friends, this design has no way to become "not friends" again.
