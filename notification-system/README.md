# Notification System

> **Mode: Learning** — this design (happy flow, class breakdown, pattern
> choices) was authored by Claude, not the user. See `CLAUDE.md` → "Mode:
> Learning" for what that implies about ownership of the decisions below.

A multi-channel notification fan-out: users subscribe to whichever
channels they want (email/SMS/push), a `Notification` is built once, and
sending it reaches every channel that user is currently subscribed to.

## Happy flow

1. `NotificationService.subscribe(userId, channelType)` registers a
   channel observer for that user.
2. A caller builds a `Notification` via `new NotificationBuilder()
   .title(...).body(...).priority(...).build()` — `build()` validates that
   both title and body were actually set.
3. `send(userId, notification)` renders the notification's content,
   wrapping it in whichever decorators apply (a signature is always added;
   an urgency prefix is added only for `HIGH` priority), then hands the
   final rendered string to every channel that user is subscribed to.

## Design patterns used

- **Builder** — `builder/NotificationBuilder.java`. `Notification` has no
  public constructor — the only way to get one is through the builder,
  which validates required fields (`title`, `body`) at `build()` time and
  defaults `priority` to `NORMAL` if never set.
- **Decorator** — `decorator/NotificationContent.java` with `PlainContent`
  (the base), `SignatureDecorator`, and `UrgentPrefixDecorator`. Each
  decorator always does its one thing when applied — it's
  `NotificationService.send()` that decides *which* decorators to stack
  for a given notification (always signature; urgency only if `HIGH`),
  keeping the decorators themselves simple and composable rather than
  each one containing its own conditional logic.
- **Observer** — `observer/NotificationChannel.java` with `EmailChannel`,
  `SmsChannel`, `PushChannel`. `SubscriptionRegistry` tracks which channels
  each user is subscribed to; `send()` just loops over whatever's
  registered for that user without knowing or caring how many there are or
  what each one does with the message.

## Structure

```
notification-system/
  src/
    model/       Notification, NotificationPriority, ChannelType
    builder/     NotificationBuilder
    decorator/   NotificationContent + Plain/Urgent/SignatureDecorator
    observer/    NotificationChannel + Email/Sms/PushChannel
    repository/  SubscriptionRegistry (in-memory)
    exceptions/  IncompleteNotificationException
    services/    NotificationService (the only public entry point)
    Main.java    Reads a command script, drives the service, writes a transcript
  test/
    input/scenario.txt   Multi-channel fan-out, a HIGH-priority urgency prefix,
                          a user with zero subscriptions (legal no-op), an
                          unsubscribe, and every guard/error path
    output/output.txt    Captured run transcript
  diagrams/
    generate.py                 Data-only script that builds notification-system.drawio
    notification-system.drawio  Class diagram + 1 sequence diagram (compose
                                 decorators, then fan out to subscribed channels)
  explainer/index.html   Interactive step-through: subscribe users to channels, then
                          send notifications and watch the real decorator stack build
                          up before fanning out to whichever channels are subscribed
```

## Running it

```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

Requires the VS Code "Extension Pack for Java" (Microsoft) for the
`.vscode/launch.json` run config to work — open the `notification-system/`
folder itself as the workspace root, then use the "Run Main (scenario.txt)"
config.

## Known gaps (flagged, not fixed)

- No persistence — all subscriptions are in-memory and lost on process exit.
- No delivery retries, rate limiting, or delivery confirmation — a channel
  "send" is a synchronous, always-succeeds `println`-style call.
- No per-channel formatting differences — the same rendered string goes to
  email, SMS, and push alike, even though a real SMS channel would want a
  much shorter message.
- No templates — `title`/`body` are always caller-supplied literal
  strings, not filled in from a reusable template with placeholders.
- `send()` doesn't distinguish "user has zero subscriptions" from "user
  doesn't exist" — both just notify 0 channels, since there's no separate
  user-registration step in this design.
