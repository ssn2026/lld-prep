# Notification System — Simple Walkthrough

This file explains the whole codebase in plain language, in the order you
should read it. No prior context assumed.

---

## 1. The story in one paragraph

A user can subscribe to any combination of channels — email, SMS, push —
and later, someone can send that user a `Notification` (a title, a body,
and a priority). Sending it doesn't go straight out as raw text: the
message first gets wrapped up ("decorated") with a signature line always,
and an urgency prefix *only* if the priority is `HIGH`. Whatever comes out
of that wrapping is the exact string handed to every channel that user
currently subscribes to. If a user has no subscriptions at all, sending
to them is legal — it just quietly reaches nobody.

---

## 2. The one door you're allowed to knock on

`src/services/NotificationService.java` is the **only** class anything
outside the package is meant to call.

| Method | What it does |
|---|---|
| `subscribe(userId, channelType)` | Register a channel for that user |
| `unsubscribe(userId, channelType)` | Remove a channel; `true` if one was actually removed |
| `send(userId, notification)` | Render + deliver to every subscribed channel, returns how many were reached |

Building a `Notification` itself isn't a method on this service at all —
it goes through its own builder, covered in Step 2.

---

## 3. Read the code in this order

### Step 1 — the plain data (`src/model/`)

- **`NotificationPriority.java`** — `LOW, NORMAL, HIGH`.
- **`ChannelType.java`** — `EMAIL, SMS, PUSH`.
- **`Notification.java`** — holds a `title`, a `body`, and a `priority`,
  all `final`. Read its one-line doc comment: *"Immutable; only ever
  constructed via builder.NotificationBuilder."* There is a public
  constructor here, but by convention nothing outside `builder/` is
  meant to call it directly — the builder is the real front door (Step
  2).

### Step 2 — building a Notification, safely (`src/builder/NotificationBuilder.java`)

This is the **Builder** pattern. `Notification` has several fields, some
optional (priority defaults if you never set it), some required (title
and body must actually be there) — a plain constructor with five
parameters would be easy to call wrong. Instead:

```java
public class NotificationBuilder {
    private String title;
    private String body;
    private NotificationPriority priority = NotificationPriority.NORMAL;

    public NotificationBuilder title(String title) { this.title = title; return this; }
    public NotificationBuilder body(String body) { this.body = body; return this; }
    public NotificationBuilder priority(NotificationPriority priority) { this.priority = priority; return this; }

    public Notification build() {
        if (title == null || title.isBlank()) throw new IncompleteNotificationException("title");
        if (body == null || body.isBlank()) throw new IncompleteNotificationException("body");
        return new Notification(title, body, priority);
    }
}
```

Each setter returns `this`, which is what lets you chain calls:
`new NotificationBuilder().title("Welcome").body("Thanks!").build()`. The
important part is `build()` — it's the **one place** that checks
`title`/`body` aren't missing (`null` or blank), and only calls
`Notification`'s real constructor once both checks pass. If you forget to
call `.body(...)` before `.build()`, you get an
`IncompleteNotificationException` naming exactly which field was missing
— you never get a half-formed `Notification` object floating around with
a `null` body.

### Step 3 — wrapping the message before it goes out (`src/decorator/`)

This is the **Decorator** pattern, and it's worth slowing down on because
the way it's *used* here is a specific, deliberate choice. First, the
shared interface:

```java
public interface NotificationContent {
    String render();
}
```

`PlainContent` is the un-decorated base — it just formats the
notification's own fields:

```java
public String render() {
    return "[" + notification.getPriority() + "] " + notification.getTitle() + ": " + notification.getBody();
}
```

`SignatureDecorator` and `UrgentPrefixDecorator` both **wrap** another
`NotificationContent` (they hold an `inner` reference to it) and add one
thing on top of whatever `inner.render()` already produced:

```java
// SignatureDecorator
public String render() { return inner.render() + "\n-- Sent by NotifyService"; }

// UrgentPrefixDecorator
public String render() { return "*** URGENT *** " + inner.render(); }
```

Here's the deliberate design choice, spelled out in
`UrgentPrefixDecorator`'s own doc comment: *"Always adds its prefix when
applied — the SERVICE decides whether to wrap with this, not the
decorator itself."* In other words, `UrgentPrefixDecorator` itself has **no
idea** what priority the notification is — it doesn't check anything, it
just unconditionally prepends its text whenever something calls
`render()` on it. The decision of *whether* to use it at all lives one
layer up, in `NotificationService.send()` (Step 5). This keeps each
decorator dead simple (one job, no conditionals) and puts the "when do we
apply this" logic in exactly one place instead of scattering priority
checks across every decorator.

### Step 4 — where a rendered message actually goes (`src/observer/`)

```java
public interface NotificationChannel {
    void send(String userId, String renderedMessage);
}
```

Three implementations — `EmailChannel`, `SmsChannel`, `PushChannel` —
each just a one-line `println` tagged with its own channel name (e.g.
`"[EMAIL -> " + userId + "] " + renderedMessage`). These are the
**Observer** pattern's concrete observers: a user can be "watched" by any
combination of them at once. `NotificationService.send()` never checks
which *kind* of channel it's talking to — it just calls `.send(userId,
rendered)` on whichever channels happen to be registered (Step 5).

### Step 5 — who's subscribed to what (`src/repository/SubscriptionRegistry.java`)

```java
private final Map<String, Map<ChannelType, NotificationChannel>> subscriptionsByUser = new LinkedHashMap<>();
```

A map of a map: `userId` → (`ChannelType` → the actual `NotificationChannel`
object for that user+type pair). Three operations:

- `subscribe(userId, type, channel)` — `computeIfAbsent` creates the
  inner map for that user the first time they subscribe to *anything*,
  then puts the new channel in under its type. Subscribing to the same
  type twice just overwrites the old channel object with a new one (same
  net effect either way, since all three channel classes are stateless).
- `unsubscribe(userId, type)` — removes one entry and returns whether
  anything was actually there to remove (`Map.remove()` returns `null` if
  the key didn't exist, so `!= null` becomes the boolean result).
- `getChannelsFor(userId)` — returns just the *values* of that user's
  inner map (i.e., the channel objects, not their types), or an empty
  list if the user has never subscribed to anything at all — never
  `null`, so callers never need a null-check.

### Step 6 — errors (`src/exceptions/IncompleteNotificationException.java`)

Only one custom exception in this problem — thrown by
`NotificationBuilder.build()` when a required field is missing (Step 2).
Note that an *unknown channel type string* (like `"FAX"`) doesn't get its
own custom exception at all — see Step 8, it surfaces as a plain
`IllegalArgumentException` from Java's built-in `Enum.valueOf()`.

### Step 7 — the orchestrator (`src/services/NotificationService.java`)

`subscribe()` is a thin wrapper that turns a `ChannelType` into the right
concrete `NotificationChannel` object via a private helper:

```java
private NotificationChannel newChannel(ChannelType type) {
    return switch (type) {
        case EMAIL -> new EmailChannel();
        case SMS -> new SmsChannel();
        case PUSH -> new PushChannel();
    };
}
```

This `switch` is the **only** place in the whole codebase that branches
on channel type — everywhere else just treats every `NotificationChannel`
uniformly through the interface.

`send()` is where the Builder, Decorator, and Observer pieces all meet:

```java
public int send(String userId, Notification notification) {
    NotificationContent content = new PlainContent(notification);
    content = new SignatureDecorator(content);
    if (notification.getPriority() == NotificationPriority.HIGH) {
        content = new UrgentPrefixDecorator(content);
    }
    String rendered = content.render();

    int count = 0;
    for (NotificationChannel channel : registry.getChannelsFor(userId)) {
        channel.send(userId, rendered);
        count++;
    }
    return count;
}
```

Read the decorator stacking line by line: start with `PlainContent`
(the notification's own fields), always wrap it in `SignatureDecorator`,
then *conditionally* wrap the result again in `UrgentPrefixDecorator` —
but **only if** the priority is `HIGH`. Only after all the wrapping is
decided does `.render()` get called exactly once, at the very end, which
cascades down through every layer (`UrgentPrefixDecorator.render()` calls
`inner.render()` which is `SignatureDecorator.render()`, which calls
*its* `inner.render()`, which is `PlainContent.render()` — the innermost
one, with no `inner` of its own to delegate to).

The final loop hands that one rendered string to every channel currently
subscribed for that user, counting how many were actually reached — which
is exactly what gets returned and printed by `Main.java` (Step 8).

### Step 8 — the runner (`src/Main.java`)

A test harness reading `test/input/scenario.txt`, driving
`NotificationService`, and writing a transcript to
`test/output/output.txt`. One thing worth knowing: the `NOTIFY` command's
`body` argument is **optional** in the script parsing — if you write a
`NOTIFY` line with no body text at all, `Main.java` simply never calls
`.body(...)` on the builder, which is exactly how the "missing body"
error path (Step 9) gets triggered naturally from realistic input, rather
than needing a special test-only command.

Also worth knowing: each `NotificationChannel`'s `println` (e.g.
`"[EMAIL -> Alice] ..."`) writes straight to the console and is **not**
captured into `test/output/output.txt` — only what `Main.java` explicitly
builds into its own transcript string makes it into the file. If you run
this yourself in a terminal, you'll see those channel lines live, even
though they don't appear in the saved file quoted below.

---

## 4. Picture of one full flow: a HIGH-priority notification, two channels

```
Main.java (reads "NOTIFY Alice HIGH ServerDown The database is unreachable")
   |
   v
Notification n = new NotificationBuilder()
                      .title("ServerDown")
                      .body("The database is unreachable")
                      .priority(HIGH)
                      .build()
   |  both title and body present -> builds successfully
   v
NotificationService.send("Alice", n)
   |  content = new PlainContent(n)
   |  content = new SignatureDecorator(content)
   |  n.getPriority() == HIGH   -> TRUE
   |       content = new UrgentPrefixDecorator(content)
   |  rendered = content.render()
   |       UrgentPrefixDecorator.render()
   |            "*** URGENT *** " + SignatureDecorator.render()
   |                 SignatureDecorator.render()
   |                      PlainContent.render() + "\n-- Sent by NotifyService"
   |                           PlainContent.render() -> "[HIGH] ServerDown: The database is unreachable"
   |                      -> "[HIGH] ServerDown: The database is unreachable\n-- Sent by NotifyService"
   |            -> "*** URGENT *** [HIGH] ServerDown: The database is unreachable\n-- Sent by NotifyService"
   |  registry.getChannelsFor("Alice")   -> [emailChannel, smsChannel]   (Alice subscribed to both earlier)
   |  for each channel: channel.send("Alice", rendered)   <- 2 iterations, count becomes 2
   v
send() returns 2
Main.java prints: "OK notified 2 channel(s) for Alice"
```

---

## 5. Reading the actual captured run (`test/output/output.txt`)

```
> SUBSCRIBE Alice EMAIL
OK Alice subscribed to EMAIL
> SUBSCRIBE Alice SMS
OK Alice subscribed to SMS
> SUBSCRIBE Bob PUSH
OK Bob subscribed to PUSH
```

Alice ends up subscribed to two channels, Bob to just one.

```
> NOTIFY Alice NORMAL Welcome Thanks for signing up!
OK notified 2 channel(s) for Alice
```

`NORMAL` priority — no `UrgentPrefixDecorator` gets applied (only
`SignatureDecorator` does, unconditionally), but the count is still `2`
because it's the *subscription count*, not anything to do with priority.

```
> NOTIFY Bob LOW WeeklyDigest Here is what you missed this week
OK notified 1 channel(s) for Bob
```

Bob only subscribed to `PUSH`, so exactly one channel is reached,
regardless of priority.

```
> NOTIFY Carol NORMAL Hello This should reach nobody
OK notified 0 channel(s) for Carol
```

Carol never called `SUBSCRIBE` at all. This is **not** an error —
`registry.getChannelsFor("Carol")` returns an empty list (per Step 5,
never `null`), the `for` loop simply runs zero times, and `send()`
correctly returns `0`. Sending to someone with no subscriptions is legal;
it just quietly reaches nobody.

```
> UNSUBSCRIBE Alice SMS
OK Alice unsubscribed from SMS
> NOTIFY Alice HIGH FollowUp SMS channel should no longer receive this
OK notified 1 channel(s) for Alice
```

Alice had two channels; after unsubscribing from `SMS`, only `EMAIL`
remains, so the very next `NOTIFY` to her reaches exactly `1` channel —
proof `unsubscribe()` genuinely removes the entry rather than just
marking it inactive.

```
> UNSUBSCRIBE Bob SMS
NOOP Bob was not subscribed to SMS
```

Bob was only ever subscribed to `PUSH`, never `SMS`. `unsubscribe()`
correctly returns `false` (no entry existed to remove), which
`Main.java` reports as `NOOP` rather than `OK` — a legal, harmless
no-op, not an exception.

```
> NOTIFY Alice NORMAL EmptyBody
ERROR IncompleteNotificationException: Cannot build a Notification without a body
```

This `NOTIFY` line supplies a title (`"EmptyBody"`) but no body text at
all. Per Step 8, `Main.java` never calls `.body(...)` on the builder in
that case, so `build()`'s own check catches the missing field.

```
> SUBSCRIBE Alice FAX
ERROR IllegalArgumentException: No enum constant model.ChannelType.FAX
```

`"FAX"` isn't one of `ChannelType`'s three real values. This error comes
straight from Java's built-in `ChannelType.valueOf("FAX")` inside
`Main.java`'s own parsing — notice it's a plain `IllegalArgumentException`,
not a custom exception class like every other error in this file. That's
a deliberate, documented simplification (see the README's "Known gaps"):
this particular problem didn't get its own wrapper exception for bad
channel-type strings, unlike `logger-service/`'s
`InvalidLogLevelException` for the analogous bad-level-string case.

---

## 6. Things to try / test yourself

Edit `test/input/scenario.txt` and re-run:
```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

1. **Subscribe the same user to the same channel type twice** (e.g.
   `SUBSCRIBE Alice EMAIL` twice in a row) — confirm `send()` still only
   reaches Alice's email channel *once* per notification, not twice
   (`SubscriptionRegistry`'s inner map only ever holds one entry per
   `ChannelType` key, so the second `subscribe()` call just overwrites
   the first).
2. **Send a `LOW` priority notification and a `HIGH` priority one to the
   same user**, and compare their rendered strings side by side (you'll
   need to watch the console output, since channel `send()` lines aren't
   captured in the file — see Step 8). Confirm only the `HIGH` one gets
   the `*** URGENT ***` prefix, and both get the signature line.
3. **Subscribe a user to all three channel types**, send them one
   notification, and confirm the reported count is `3`.
4. **Try building a `Notification` with a blank (not `null`, just empty
   or whitespace) title**, e.g. by editing `Main.java` temporarily to
   call `.title("   ")`. Confirm `isBlank()` catches it the same way a
   fully missing title would — this is a stricter check than just
   `== null`.
5. **Remove the `if (notification.getPriority() == HIGH)` condition
   entirely** in `NotificationService.send()` (always wrap with
   `UrgentPrefixDecorator`) and rerun. Every notification, regardless of
   priority, should now show the urgent prefix — confirming the decorator
   itself really has no priority logic of its own; it only ever does what
   the service tells it to.
