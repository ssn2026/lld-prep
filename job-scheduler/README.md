# Job Scheduler

> **Mode: Learning** — this design (happy flow, class breakdown, pattern
> choices) was authored by Claude, not the user. See `CLAUDE.md` → "Mode:
> Learning" for what that implies about ownership of the decisions below.

A background job scheduler: clients submit a `Job` (some work to run) with a
`Trigger` (when, and how often), and the scheduler runs it on time — once or
repeatedly — without the caller having to manage any threads themselves.
See `WALKTHROUGH.md` for a step-by-step, plain-language explanation of the
whole system, aimed at someone new to concurrency.

## Happy flow

1. A client calls `JobSchedulerService.scheduleJob(jobId, job, trigger)`. The
   service computes the first due time from the trigger and stores the job
   as a `ScheduledTask`.
2. A background dispatcher thread is always waiting on a `DelayQueue`, which
   only hands back a task once its due time has arrived — no manual polling.
3. When a task comes due, the dispatcher hands it to a worker thread pool
   (`ExecutorService`) and immediately goes back to waiting for the *next*
   due task, so one slow job never blocks the rest of the schedule.
4. The worker thread runs `job.execute()`, marks the task `COMPLETED` or
   `FAILED`, and notifies any registered `JobListener`s.
5. If the trigger `isRecurring()`, the worker computes the next due time and
   re-queues a **new** `ScheduledTask` under the same job id.
6. A client can call `cancelJob(jobId)` at any time; this is a best-effort,
   lazy cancellation — see below.

## Design patterns used

- **Command** — `model/Job.java` is a single-method interface
  (`execute()`). It doesn't know when it runs or how often; it's purely "the
  work," decoupled from the scheduling machinery around it.
- **Strategy** — `strategy/Trigger.java` with `OneTimeTrigger` and
  `FixedRateTrigger`. Deciding *when* a job next runs is pulled out of both
  `Job` and `JobSchedulerService`, so a new scheduling policy (e.g. a
  cron-style trigger) only means adding one more class, never touching the
  dispatcher.
- **Observer** — `observer/JobListener.java` with `ConsoleJobListener`
  (shipped) and `Main`'s own `TranscriptListener` (client-supplied). The
  scheduler notifies on completion/failure without knowing who's listening
  or why — logging, metrics, and the test harness's own transcript all plug
  in the same way.

## Concurrency design (the actual point of this problem)

- **`DelayQueue<ScheduledTask>`** — the core data structure. `take()` blocks
  the dispatcher thread until the head element's delay has expired, so
  "wait efficiently for the next due job" needs zero manual sleep/poll code.
- **`ConcurrentHashMap`** (inside `repository/JobRegistry.java`) — jobId ->
  current `ScheduledTask`, giving O(1) cancellation/status lookup instead of
  scanning the queue.
- **Lazy cancellation** — `cancelJob()` never touches the `DelayQueue`
  directly. It just flips flags on the `ScheduledTask` object shared between
  the registry and the queue; the dispatcher checks those flags itself when
  the task is popped, and simply drops it instead of running it. This is the
  same technique `ScheduledThreadPoolExecutor` uses internally.
- **Immutable delay key** — `ScheduledTask.executionTimeMillis` is `final`.
  A recurring job's next occurrence is a **new** `ScheduledTask`
  (`withNextExecution`), never a mutation of the one that just ran — mutating
  an element already sitting inside a heap-backed queue corrupts its
  ordering.
- **`ExecutorService`** (fixed thread pool) — actually executes jobs, kept
  separate from the single dispatcher thread so a slow/blocking job can't
  stall the rest of the schedule.

## Structure

```
job-scheduler/
  src/
    model/       Job (Command), JobStatus, ScheduledTask (Delayed)
    strategy/    Trigger, OneTimeTrigger, FixedRateTrigger
    observer/    JobListener, ConsoleJobListener
    repository/  JobRegistry (in-memory, ConcurrentHashMap-backed)
    exceptions/  JobNotFoundException, DuplicateJobIdException
    services/    JobSchedulerService (the only public entry point)
    Main.java    Reads a command script, drives the service, writes a transcript
  test/
    input/scenario.txt   One-time jobs, a failing job, a recurring job, cancellation
                          (before and mid-schedule), duplicate-id and not-found errors
    output/output.txt    Captured run transcript
  diagrams/
    generate.py          Data-only script that builds job-scheduler.drawio
    job-scheduler.drawio Class diagram + 2 sequence diagrams (schedule/dispatch/execute,
                          recurring reschedule + cancel)
  WALKTHROUGH.md  Plain-language, ordered walk through every moving part
```

## Running it

```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

Requires the VS Code "Extension Pack for Java" (Microsoft) for the
`.vscode/launch.json` run config to work — open the `job-scheduler/` folder
itself as the workspace root, then use the "Run Main (scenario.txt)" config.

## Known gaps (flagged, not fixed)

- No persistence — all schedule state is in-memory and lost on process exit.
- The `JobRegistry` never evicts completed/failed one-time jobs, so it grows
  unbounded over a long-running process — fine for this demo's scale, not
  for production.
- `cancelJob()` cannot interrupt a job that's already running; it only
  prevents a not-yet-started occurrence and stops future recurrences.
- No retry/backoff policy for failed jobs — a failure is reported to
  listeners and, for a recurring job, simply doesn't block the next
  scheduled occurrence from happening anyway.
- Fixed-size worker pool with no backpressure — if jobs come due faster than
  the pool can drain them, submitted work just queues up inside the
  `ExecutorService`'s internal queue.
- Wall-clock based: the test scenario relies on real `Thread.sleep`s with
  generous margins to stay non-flaky, not simulated/virtual time.
