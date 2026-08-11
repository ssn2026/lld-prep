# Calendar

> **Mode: Learning** — this design (happy flow, class breakdown, pattern
> choices) was authored by Claude, not the user. See `CLAUDE.md` → "Mode:
> Learning" for what that implies about ownership of the decisions below.

A shared calendar system: users schedule one-off or recurring events with
attendees, conflicts are detected across every invited calendar, viewers see
either full detail or a privacy-redacted "Busy" block depending on whether
they're invited, and calendars can be merged into (possibly nested) team
groups for an aggregated view.

## Happy flow

1. Users register (`CalendarService.registerUser()`), each getting an empty
   personal `Calendar`.
2. A user schedules an event (`createEvent()`) naming attendees and an
   optional recurrence (`DAILY`/`WEEKLY`/`MONTHLY` for N occurrences). The
   matching `RecurrenceStrategy` expands the first occurrence into the full
   list of start times.
3. Every occurrence is checked for conflicts against every participant's
   calendar (owner + attendees) — and against the *other* occurrences of the
   same new series — before anything is saved. Only if the whole batch is
   conflict-free does it get committed to every participant's calendar at
   once.
4. Anyone can view any user's calendar (`viewCalendar()`). The owner and any
   invited attendee see full event details; everyone else sees only a
   redacted "Busy" block with the correct time range.
5. Events can be cancelled individually (`cancelEvent()`) or as a whole
   series (`cancelSeries()`), removing them from every participant's
   calendar.
6. A team lead can build a `CalendarGroup` (`createGroup()` +
   `addGroupMember()`) that merges several users' calendars — or other
   groups, nested arbitrarily — into one aggregated event list
   (`getGroupEvents()`).

## Design patterns used

- **Composite** — `composite/CalendarComponent.java` (interface) with
  `model/Calendar.java` as the leaf and `composite/CalendarGroup.java` as
  the composite. Both answer `getEvents()`/`isBusy()` the same way, so a
  `CalendarGroup` can contain either individual calendars or other groups
  interchangeably (`test/input/scenario.txt`'s `Org` group nests the
  `TeamCal` group alongside a standalone user) without `CalendarService`
  ever branching on which kind of member it's looking at.
  `CalendarGroup.getEvents()` merges members through a `LinkedHashSet`
  (using `Event.equals()`/`hashCode()` by `eventId`), so a meeting shared by
  two members of the same group is deduped rather than listed twice.
- **Proxy** — `proxy/RestrictedCalendarProxy.java`, a protection proxy that
  wraps the real `Calendar` and implements the same `CalendarComponent`
  interface. `CalendarService.viewCalendar()` always looks through this
  proxy, so the "owner/attendee sees details, everyone else sees only
  Busy" rule lives in exactly one place (`canViewDetails()`) instead of
  being re-checked by every caller. Busy/free status itself is never
  restricted — only `getEvents()`'s detail is swapped for
  `Event.redacted()`.
- **Strategy** — `strategy/RecurrenceStrategy.java` with
  `SingleOccurrenceStrategy`/`DailyRecurrenceStrategy`/
  `WeeklyRecurrenceStrategy`/`MonthlyRecurrenceStrategy`. `createEvent()`
  picks the strategy for the requested `RecurrenceType` and otherwise
  treats every kind of recurrence identically — generate the occurrence
  starts, then run the same conflict-check-then-commit flow regardless of
  which algorithm produced them.

## Structure

```
calendar/
  src/
    model/       User, Event, RecurrenceType, Calendar (Composite leaf)
    composite/   CalendarComponent, CalendarGroup
    proxy/       RestrictedCalendarProxy
    strategy/    RecurrenceStrategy family (Single/Daily/Weekly/Monthly)
    repository/  UserRepository, CalendarRepository, EventRepository, CalendarGroupRepository (in-memory)
    exceptions/  UserNotFoundException, EventConflictException, EventNotFoundException,
                 InvalidEventException, CalendarGroupNotFoundException
    services/    CalendarService (the only public entry point)
    Main.java    Reads a command script, drives the service, writes a transcript
  test/
    input/scenario.txt   Recurring events, conflicts, privacy views, nested groups, and every edge case below
    output/output.txt    Captured run transcript
  diagrams/
    generate.py       Data-only script that builds calendar.drawio via docs/tooling/drawio_uml.py
    calendar.drawio    Class diagram + 3 sequence diagrams (create event, view via proxy, nested group view)
  explainer/index.html   Interactive step-through: schedule a recurring event and tap "Next step" to watch
                          createEvent()'s Strategy expansion and plan-then-commit conflict check run, then
                          compare an attendee's vs a non-attendee's Proxy-redacted view of the same calendar
                          (open directly in a browser)
```

## Running it

```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

Requires the VS Code "Extension Pack for Java" (Microsoft) for the
`.vscode/launch.json` run config to work — open the `calendar/` folder
itself as the workspace root, then use the "Run Main (scenario.txt)"
config. Event titles in the test script use underscores instead of spaces
(e.g. `Team_Standup`) to keep the whitespace-delimited command parsing
simple.

## Known gaps (flagged, not fixed)

- No persistence — all state is in-memory and lost on process exit.
- No concurrency control — `createEvent()`'s plan-then-commit split isn't
  atomic across threads; two concurrent bookings could both pass the
  conflict check before either commits.
- Recurrence has no end date or exception-date support (skipping/moving one
  occurrence of a series) — only a flat occurrence count.
- `CalendarGroup.isBusy()` is a simple OR across members ("is *anyone* in
  this group busy") — there's no "find the next slot everyone is free"
  scheduling assistant built on top of it.
- Group views (`getGroupEvents()`) don't go through `RestrictedCalendarProxy`
  — they show full event details unconditionally, on the assumption that
  whoever can call `getGroupEvents()` already has team-lead-level
  visibility. A real system would want proxy-wrapped members here too.
- `Event`'s time fields are plain `LocalDateTime` — no time zone handling,
  so all users are implicitly assumed to share one zone.
