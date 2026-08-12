# Calendar — Simple Walkthrough

This file explains the whole codebase in plain language, in the order you
should read it. No prior context assumed.

---

## 1. The story in one paragraph

You have a shared calendar system for a group of users. Anyone can schedule
a one-off meeting or a recurring one (e.g. "daily standup for 3 days"), and
they invite other users as attendees. The system needs to make sure nobody
double-books — if you try to schedule a meeting that overlaps something
already on your calendar, or on any attendee's calendar, the whole booking
gets rejected instead of half-succeeding. Separately, when someone looks at
another person's calendar, what they see depends on who they are: the owner
and anyone invited to a specific meeting see its real title and details,
but everyone else only sees that the owner is "Busy" during that time
(their privacy is protected, but their free/busy status is still visible so
scheduling around them still works). On top of individual calendars, you
can also build team-level views by grouping several users' calendars
together — and those groups can themselves be nested inside bigger groups.

---

## 2. The one door you're allowed to knock on

`src/services/CalendarService.java` is the **only** class anything outside
the package is meant to call. Everything else (`model`, `composite`,
`proxy`, `strategy`, `repository`, `exceptions`) is a helper it uses
internally.

| Method | What it does |
|---|---|
| `registerUser(userId, name)` | Create a user and their (initially empty) personal calendar |
| `createEvent(ownerId, title, description, start, end, attendeeIds, recurrenceType, occurrenceCount)` | Schedule a one-off or recurring event, conflict-checked across every participant first |
| `cancelEvent(eventId)` | Remove a single occurrence from every participant's calendar |
| `cancelSeries(seriesId)` | Remove every occurrence of a recurring series |
| `viewCalendar(viewerId, ownerId, rangeStart, rangeEnd)` | See another user's calendar, privacy-filtered based on who's asking |
| `createGroup(groupId)` | Create an empty team/group calendar |
| `addGroupMember(groupId, memberId)` | Add a user's calendar — or another existing group — as a member |
| `getGroupEvents(groupId, rangeStart, rangeEnd)` | Aggregated event list across every member (and nested member) of a group |

---

## 3. Read the code in this order

### Step 1 — the building blocks (`src/model/`)

- **`User.java`** — just an id and a name. No behavior.
- **`RecurrenceType.java`** — an enum: `NONE`, `DAILY`, `WEEKLY`, `MONTHLY`.
  Just a label; the actual "how do the occurrences repeat" logic lives in
  `strategy/` (Step 3).
- **`Event.java`** — an id, title, description, start/end time, an owner,
  a set of attendee ids, and (for recurring events) a `seriesId` shared
  across every occurrence of the same series — `null` for a one-off event.
  Two details worth knowing:
  - `redacted()` returns a **new** `Event` with the same id/timing/owner
    but the title replaced with `"Busy"`, the description dropped, and the
    attendees emptied out. This is what the privacy feature (Step 4 below)
    actually swaps in for viewers who shouldn't see details.
  - `equals()`/`hashCode()` are defined purely by `eventId`. This matters
    a lot in `CalendarGroup` (below): if the same event shows up via two
    different group members (e.g. both the owner's and an attendee's
    calendar), it needs to be recognized as "the same event" and deduped,
    not listed twice.
- **`Calendar.java`** — one user's personal calendar: a map from event id
  to `Event`. It has two read-only query methods that matter a lot later:
  `hasConflict(start, end)` (does any stored event overlap this time
  range?) and `getEvents(rangeStart, rangeEnd)` (which events fall inside
  a given window, sorted by start time). `Calendar` also implements an
  interface, `CalendarComponent` — that's the Composite pattern, covered
  next.

### Step 2 — one calendar or many, treated the same way (`src/composite/`)

This is the **Composite** pattern: the whole point is that "a single
person's calendar" and "a merged view of several calendars (a team, or
even a team-of-teams)" should be askable the exact same two questions —
"what events do you have in this range?" and "are you busy right now?" —
without the caller needing to know or care which kind it's talking to.

- **`CalendarComponent.java`** — the shared interface: `getEvents(rangeStart,
  rangeEnd)` and `isBusy(instant)`. `Calendar` (a **leaf** — an individual,
  non-divisible calendar) implements this directly, as you saw in Step 1.
- **`CalendarGroup.java`** — the **composite**: it holds a `List` of
  `CalendarComponent` members, and each member can be either an individual
  `Calendar` or *another* `CalendarGroup`. Because both kinds satisfy the
  same interface, a group can nest other groups arbitrarily — the test
  scenario builds exactly this, with an `Org` group whose members are a
  `TeamCal` group and a standalone user (`carol`) side by side.
  - `getEvents()` walks every member, calls `member.getEvents(...)` on
    each (whether that member is a leaf or another group makes no
    difference — this is where the pattern actually pays off), and merges
    all the results into a `LinkedHashSet<Event>` before sorting — the
    `Set` is what causes an event visible through two different members
    (say, two teammates both invited to the same meeting) to appear only
    once, relying on `Event.equals()`/`hashCode()` from Step 1.
  - `isBusy()` is a simple OR across members: the group counts as "busy"
    if *any* single member is busy at that instant.

### Step 3 — how many times does an event repeat? (`src/strategy/`)

This is the **Strategy** pattern: "how do you turn one first occurrence
into a full list of occurrence start times" is a swappable algorithm, and
`CalendarService.createEvent()` doesn't need a different code path per
recurrence type — it just asks whichever strategy matches the requested
`RecurrenceType` to `generateStartTimes(...)`, then runs the exact same
conflict-check-then-commit logic regardless of which one answered.

- **`RecurrenceStrategy.java`** — the interface: one method,
  `generateStartTimes(firstStart, occurrenceCount)`.
- **`SingleOccurrenceStrategy.java`** — for `RecurrenceType.NONE`: always
  returns a list containing just the one `firstStart` — `occurrenceCount`
  is ignored entirely for a one-off event.
- **`DailyRecurrenceStrategy.java`** — returns `firstStart.plusDays(i)` for
  `i` from `0` to `occurrenceCount - 1`.
- **`WeeklyRecurrenceStrategy.java`** — same shape, `plusWeeks(i)`.
- **`MonthlyRecurrenceStrategy.java`** — same shape, `plusMonths(i)`.

All three recurring strategies are essentially the same 6-line loop with a
different `Duration`-style increment — the value of splitting them into
separate classes isn't algorithmic complexity, it's that adding a new
recurrence type later (say, "every weekday") means writing one new small
class, not adding another branch to a growing `if`/`switch` buried inside
`createEvent()`.

### Step 4 — who's allowed to see what (`src/proxy/RestrictedCalendarProxy.java`)

This is the **Proxy** pattern — specifically a "protection proxy," which
means an object that stands in front of a real object and enforces an
access-control rule before letting a call through, without the real object
(`Calendar`) needing to know anything about permissions itself.

`RestrictedCalendarProxy` implements the same `CalendarComponent` interface
as `Calendar` and `CalendarGroup` (so, again, the caller can't tell the
difference just from the type). It wraps a real `Calendar` plus a
`viewerId` — the id of whoever is asking to look at this calendar. Its
`getEvents()` calls through to the real calendar's `getEvents()`, then for
each returned event calls a private helper, `canViewDetails(event)`, which
is true only if the viewer **is** the calendar's owner or **is** listed in
that specific event's attendees. If `canViewDetails` is false, the proxy
substitutes `event.redacted()` (from Step 1) instead of the real event.
Notice `isBusy()` is passed straight through to the real calendar,
unfiltered — free/busy status is deliberately never restricted, only the
event details are, so other people can still tell you're unavailable
without learning why.

Because `CalendarService.viewCalendar()` (Step 6 below) *always* wraps the
real `Calendar` in this proxy before returning anything to a caller, the
"who can see what" rule exists in exactly one place in the whole codebase —
no other method needs to re-implement or remember to apply it.

### Step 5 — looking things up (`src/repository/`)

Four small repositories, each a thin wrapper around a
`Map<String, SomeType>` with save/find(/exists) operations, and each
throwing its own not-found exception on a miss:

- **`UserRepository.java`** — `findByUserId` throws `UserNotFoundException`.
- **`CalendarRepository.java`** — keyed by owner id.
  `findByOwnerId` also throws `UserNotFoundException` (note: not a
  "calendar not found" exception — since every registered user gets a
  calendar automatically, a missing calendar and a missing user are treated
  as the same underlying problem). It also exposes `exists(ownerId)`, used
  by `CalendarService.addGroupMember()` to tell "is this a user id" apart
  from "is this a group id."
- **`EventRepository.java`** — keyed by event id;
  `findByEventId` throws `EventNotFoundException`; `findBySeriesId` scans
  every stored event for a matching `seriesId` (there's no secondary index
  by series — this is a linear scan, acceptable at this scale).
- **`CalendarGroupRepository.java`** — keyed by group id;
  `findByGroupId` throws `CalendarGroupNotFoundException`; `exists(groupId)`
  is the group-side counterpart used by the same `addGroupMember()` check.

### Step 6 — errors (`src/exceptions/`)

Five plain `RuntimeException` subclasses, each just wrapping a message —
their value is letting `Main.java`'s catch clause (and any future caller)
distinguish failure kinds by Java type:

- `UserNotFoundException` — unknown user (or, by extension, unknown
  calendar owner).
- `EventConflictException` — a new occurrence overlaps an existing event on
  some participant's calendar.
- `EventNotFoundException` — `cancelEvent`/`cancelSeries` referencing an id
  that doesn't exist.
- `InvalidEventException` — malformed input: end time not after start time,
  or a recurring series whose own occurrences overlap each other.
- `CalendarGroupNotFoundException` — unknown group id.

### Step 7 — the orchestrator (`src/services/CalendarService.java`)

This is where every earlier piece gets wired together. The most important
method by far is `createEvent()` — read it carefully, since it's a
textbook example of "validate everything, then commit everything, with
nothing in between":

1. **Basic validation.** `end.isAfter(start)` — reject immediately if not.
2. **Resolve participants.** Every attendee id plus the owner id, deduped
   into a `LinkedHashSet<String> participants` — and every one of them
   must already be a registered user (`userRepository.findByUserId`), or
   the whole call fails right there.
3. **Expand the recurrence.** Pick the `RecurrenceStrategy` matching the
   requested `RecurrenceType` (`resolveStrategy()`) and call
   `generateStartTimes(start, occurrenceCount)` to get the full list of
   occurrence start times.
4. **Check the new occurrences don't overlap each other.**
   `requireOccurrencesDontOverlapEachOther()` — this catches the case
   where the event's own duration is longer than its recurrence interval
   (e.g. a 34-hour "daily" event, where occurrence 2 would start before
   occurrence 1 even ends).
5. **Check every occurrence against every participant's *existing*
   calendar.** A nested loop: for each occurrence start time, for each
   participant, call `calendarRepository.findByOwnerId(participantId)
   .hasConflict(occurrenceStart, occurrenceEnd)`. The **first** conflict
   found anywhere throws `EventConflictException` immediately — nothing
   has been saved yet at this point.
6. **Only now, after every check above has passed, build and commit.**
   Create one `Event` object per occurrence (all sharing one new
   `seriesId` if this is a recurring type, or `seriesId = null` for
   `NONE`), save each into `eventRepository`, and add each into every
   participant's `Calendar` via `addEvent()`.

The comment directly above the method spells out exactly why steps 1-5 are
kept entirely separate from step 6: *"a conflict on occurrence 3 of 4 must
not leave 1-2 half-booked."* This "plan fully, then commit fully" shape is
the same idea you'll recognize from the ATM problem's cash-dispensing
chain, applied here to calendar bookings instead of banknotes.

The rest of the class is comparatively simple:

- `cancelEvent()`/`cancelSeries()` both funnel through a shared private
  helper, `removeFromAllCalendars(event)`, which — mirroring
  `createEvent()`'s participant-resolution logic — rebuilds the
  owner-plus-attendees set for that event and calls `removeEvent(eventId)`
  on each participant's calendar.
- `viewCalendar()` looks up the real `Calendar`, wraps it in a
  `new RestrictedCalendarProxy(calendar, viewerId)`, and calls
  `getEvents()` on the proxy — never on the real calendar directly.
- `addGroupMember()` decides whether `memberId` refers to a user or an
  existing group by checking `groupRepository.exists(memberId)` first —
  if it's an existing group id, that group gets nested in as a member;
  otherwise it's treated as a user id and that user's `Calendar` is looked
  up and added instead. This one `exists()` check is what lets `GROUPADD
  Org TeamCal` (nesting a group) and `GROUPADD TeamCal alice` (adding an
  individual) go through the identical method.

### Step 8 — the runner (`src/Main.java`)

Not part of the "real" system — a test harness. It reads a text file line
by line (`test/input/scenario.txt`), turns each line into a call on
`CalendarService`, and writes what happened to `test/output/output.txt`.
Its command language: `USER`, `EVENT`, `CANCEL`, `CANCELSERIES`, `VIEW`,
`GROUP`, `GROUPADD`, `GROUPVIEW`. One parsing quirk worth knowing: event
titles in the script use underscores instead of spaces (e.g.
`Team_Standup`) because the whole line is split on whitespace — `Main`
converts underscores back to spaces (`parts[2].replace('_', ' ')`) before
handing the title to `createEvent()`.

---

## 4. Order of operations — two traces through the real code

### Trace A — scheduling a recurring event, then two different views of it

```
Main.java: "EVENT alice Standup 2026-08-10T09:00 2026-08-10T09:30 bob,carol DAILY 3"
   |
   v
CalendarService.createEvent("alice", "Standup", null, 09:00, 09:30, {bob,carol}, DAILY, 3)
   | end.isAfter(start) -- OK
   | participants = {bob, carol, alice}   -- LinkedHashSet, owner added last
   | every participant must exist -- all three do
   | strategy = resolveStrategy(DAILY) -> new DailyRecurrenceStrategy()
   | occurrenceStarts = strategy.generateStartTimes(2026-08-10T09:00, 3)
   |     -> [2026-08-10T09:00, 2026-08-11T09:00, 2026-08-12T09:00]
   | requireOccurrencesDontOverlapEachOther(...) -- 30-min events, 1-day apart -- OK
   | for each occurrenceStart, for each participant:
   |     calendarRepository.findByOwnerId(participant).hasConflict(...) -- all false, calendars are empty
   | seriesId = "SERIES1"
   | build 3 Event objects: E1 (Aug 10), E2 (Aug 11), E3 (Aug 12), all series=SERIES1
   | for each event: eventRepository.save(event); add to alice's, bob's, and carol's Calendar
   v
returns [E1, E2, E3]
Main.java prints "OK created 3 occurrence(s): E1 E2 E3"

Main.java: "VIEW carol bob 2026-08-10T00:00 2026-08-13T00:00"
   v
CalendarService.viewCalendar("carol", "bob", ...)
   | userRepository.findByUserId("carol") -- exists
   | calendar = calendarRepository.findByOwnerId("bob")
   | view = new RestrictedCalendarProxy(calendar, "carol")
   | view.getEvents(rangeStart, rangeEnd)
   |    realCalendar.getEvents(...) -> [E1, E2, E3]  (all inside the range)
   |    for each event: canViewDetails(event)?
   |        viewerId("carol").equals(realCalendar.getOwnerId()="bob")? no
   |        event.getAttendeeIds().contains("carol")? yes (carol was invited)
   |        -> true, keep the real event, not redacted()
   v
returns [E1, E2, E3] with full titles -- carol sees Standup, not Busy, on bob's calendar

Main.java: "VIEW dave alice 2026-08-10T00:00 2026-08-13T00:00"
   v
CalendarService.viewCalendar("dave", "alice", ...)
   | view = new RestrictedCalendarProxy(alice's calendar, "dave")
   | canViewDetails(E1)? dave isn't alice, dave isn't in {bob, carol} -- false
   | -> event.redacted() used instead: title becomes "Busy", attendees emptied
   v
returns 3 events, each showing "Busy" instead of "Standup"
```

### Trace B — a nested group view

```
Main.java script:
  GROUP TeamCal
  GROUPADD TeamCal alice
  GROUPADD TeamCal bob
  GROUP Org
  GROUPADD Org TeamCal
  GROUPADD Org carol
  GROUPVIEW Org 2026-08-01T00:00 2026-09-01T00:00

CalendarService.addGroupMember("Org", "TeamCal")
   | groupRepository.exists("TeamCal") -- true (it's a group id, not a user id)
   | member = groupRepository.findByGroupId("TeamCal")   -- the CalendarGroup itself
   | Org's CalendarGroup.addMember(TeamCal)   -- Org now nests TeamCal as a member

CalendarService.getGroupEvents("Org", rangeStart, rangeEnd)
   v
groupRepository.findByGroupId("Org").getEvents(rangeStart, rangeEnd)
   | Org.members = [TeamCal (a CalendarGroup), carol's Calendar (a leaf)]
   | for TeamCal: TeamCal.getEvents(...)
   |     TeamCal.members = [alice's Calendar, bob's Calendar]
   |     merges both members' events into TeamCal's own LinkedHashSet, dedupes, sorts
   |     returns that merged, sorted list up to Org
   | for carol's Calendar: Calendar.getEvents(...) -- carol's own events directly
   | Org merges BOTH results (TeamCal's already-merged list, plus carol's) into
   |     its own LinkedHashSet -- deduping again in case the same event reached
   |     Org through two different paths -- then sorts by start time
   v
returns one flat, deduped, chronologically sorted list spanning alice, bob, and carol
```

Neither `CalendarService` nor `CalendarGroup` needed a single `instanceof`
check to make this nesting work — `Org.getEvents()` calls
`member.getEvents(...)` on `TeamCal` exactly the same way it calls it on
`carol`'s plain `Calendar`, because both satisfy `CalendarComponent`.

---

## 5. Reading the actual captured run (`test/output/output.txt`)

The same 3-occurrence Standup series, seen three different ways:

```
> VIEW alice alice 2026-08-10T00:00 2026-08-13T00:00
VIEW alice (as seen by alice)
  E1 Standup [2026-08-10T09:00 - 2026-08-10T09:30] series=SERIES1
  E2 Standup [2026-08-11T09:00 - 2026-08-11T09:30] series=SERIES1
  E3 Standup [2026-08-12T09:00 - 2026-08-12T09:30] series=SERIES1
```

Alice, the owner, sees everything. Next, an invited attendee looking at
someone *else's* calendar:

```
> VIEW carol bob 2026-08-10T00:00 2026-08-13T00:00
VIEW bob (as seen by carol)
  E1 Standup [2026-08-10T09:00 - 2026-08-10T09:30] series=SERIES1
```

(and E2, E3 the same) — carol still sees the real title, because she's a
listed attendee on this specific event, even though it's bob's calendar,
not hers. Then a non-attendee:

```
> VIEW dave alice 2026-08-10T00:00 2026-08-13T00:00
VIEW alice (as seen by dave)
  E1 Busy [2026-08-10T09:00 - 2026-08-10T09:30] series=SERIES1
```

Same event id, same exact time range, but the title is now `Busy` — proof
`RestrictedCalendarProxy.getEvents()` really did substitute
`event.redacted()` for dave, while keeping the timing intact so dave can
still tell alice is unavailable then.

The conflict check working end to end:

```
> EVENT bob Lunch 2026-08-11T09:15 2026-08-11T09:45 NONE NONE 1
ERROR EventConflictException: Event conflicts with an existing event on bob's calendar at 2026-08-11T09:15
```

Bob's Aug-11 standup occurrence (E2) runs 09:00–09:30; this new "Lunch"
starts at 09:15, inside that window, so `Calendar.hasConflict()` correctly
flags the overlap and the whole booking is rejected before anything is
saved — proven by the very next line succeeding cleanly with a
non-overlapping time:

```
> EVENT bob Lunch 2026-08-11T12:00 2026-08-11T13:00 NONE NONE 1
OK created 1 occurrence(s): E8
```

The self-overlapping recurrence guard:

```
> EVENT alice Marathon 2026-09-01T09:00 2026-09-02T19:00 NONE DAILY 2
ERROR InvalidEventException: Recurring occurrences overlap each other (duration exceeds recurrence interval)
```

A 34-hour event (`09:00` on Sep 1 to `19:00` on Sep 2) recurring `DAILY`
would have its second occurrence start at Sep 2 09:00 — before the first
occurrence even ends at Sep 2 19:00 — so
`requireOccurrencesDontOverlapEachOther()` catches this before touching any
calendar.

Cancellation, single occurrence then whole series:

```
> CANCEL E2
OK cancelled event E2
> VIEW alice alice 2026-08-10T00:00 2026-08-13T00:00
VIEW alice (as seen by alice)
  E1 Standup [2026-08-10T09:00 - 2026-08-10T09:30] series=SERIES1
  E3 Standup [2026-08-12T09:00 - 2026-08-12T09:30] series=SERIES1
> CANCELSERIES SERIES1
OK cancelled series SERIES1
> VIEW alice alice 2026-08-10T00:00 2026-08-13T00:00
VIEW alice (as seen by alice)
  (no events)
```

E2 alone disappears first (E1 and E3 remain); after cancelling the whole
series, even E1 and E3 — which were never individually cancelled — are
gone too, because `cancelSeries()` looks up every event sharing
`seriesId=SERIES1` via `EventRepository.findBySeriesId()` and removes each
one.

The nested group view:

```
> GROUPVIEW Org 2026-08-01T00:00 2026-09-01T00:00
GROUPVIEW Org
  E6 Rent Reminder [2026-08-01T08:00 - 2026-08-01T08:15] series=SERIES3
  E8 Lunch [2026-08-11T12:00 - 2026-08-11T13:00]
  E4 Weekly 1 on 1 [2026-08-14T14:00 - 2026-08-14T14:30] series=SERIES2
  E5 Weekly 1 on 1 [2026-08-21T14:00 - 2026-08-21T14:30] series=SERIES2
```

E6 belongs to carol (a direct `Org` member), E8/E4/E5 come from bob (a
member of the nested `TeamCal` group) — one flat, chronologically sorted
list spanning both a direct member and a member reached only through
nesting, exactly as Trace B walks through.

Every edge case at the end of the script produces a distinct, correctly
typed error:

```
> EVENT alice Bad_Event 2026-08-10T10:00 2026-08-10T09:00 NONE NONE 1
ERROR InvalidEventException: Event end must be after start
> EVENT ghost Oops 2026-08-10T10:00 2026-08-10T11:00 NONE NONE 1
ERROR UserNotFoundException: No user with id ghost
> GROUPVIEW Ghost 2026-08-10T00:00 2026-08-13T00:00
ERROR CalendarGroupNotFoundException: No calendar group with id Ghost
```

---

## 6. Things to try / test yourself

You can edit `test/input/scenario.txt` and re-run with:
```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

1. **Invite yourself into someone else's private meeting by mistake.**
   Add a `VIEW <someone not owner or attendee> <owner>` line for one of the
   Standup occurrences and confirm you get `Busy`, not the real title —
   then add that person to the event's attendees and confirm the same
   `VIEW` now shows the real title.
2. **Book two recurring events on the same person that don't conflict
   individually but do together.** Try scheduling a second `DAILY` series
   for bob that lands exactly on one of his existing Standup times, and
   confirm the *entire* new series is rejected (not just the one
   conflicting occurrence) — because `createEvent()` checks every
   occurrence before committing any.
3. **Nest a group three levels deep.** Create `GROUP Company`, `GROUPADD
   Company Org` (nesting `Org`, which already nests `TeamCal`), and
   `GROUPVIEW Company ...` — confirm every user's events across all three
   levels show up once each, proving `CalendarGroup.getEvents()`'s
   recursion and deduping work at arbitrary depth.
4. **Cancel one occurrence of a series, then try to `CANCEL` it again.**
   `CANCEL E2` twice in a row. The second should throw
   `EventNotFoundException` — trace it to `EventRepository.findByEventId`.
5. **Check `isBusy()` semantics on a group.** This isn't wired into
   `Main`'s command language directly, but reading `CalendarGroup.isBusy()`
   and `Calendar.isBusy()` together, work out by hand what
   `TeamCal.isBusy(2026-08-11T09:15)` would return given alice and bob's
   calendars in the test data, and why it's an OR (any member busy = group
   busy) rather than an AND.
6. **Try a recurrence whose per-occurrence duration exactly equals the
   recurrence interval** (e.g. a 24-hour `DAILY` event) instead of
   exceeding it. Confirm `requireOccurrencesDontOverlapEachOther()` treats
   back-to-back-with-no-gap as *not* a conflict (the interval check uses
   `isBefore`, a strict inequality) — a good way to see exactly where the
   overlap boundary is drawn.
