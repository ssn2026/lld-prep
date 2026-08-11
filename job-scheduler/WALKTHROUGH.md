# Job Scheduler — Simple Walkthrough

This file explains the whole codebase in plain language, in the order you
should read it. No prior concurrency knowledge assumed — every data
structure and every threading concept is explained the first time it shows
up, in simple words, before you see it used in code.

---

## 1. The story in one paragraph

You want to say "run this piece of work in 5 minutes" or "run this every 10
seconds" and then walk away — the caller doesn't sit there waiting, and
doesn't have to keep checking a clock. Somewhere in the background, the
scheduler has to actually notice when 5 minutes are up and run the work at
that exact moment, possibly while ten other jobs are also waiting for their
own moments, some of which repeat forever. That "notice the right moment,
for many jobs at once, without wasting effort checking the clock
constantly" is the entire hard problem this system solves.

---

## 2. First: what does "concurrency" even mean here, and why do we need it?

Normal code you write runs **one line at a time, in order** — that's called
"single-threaded." If line 3 says `Thread.sleep(5000)` (wait 5 seconds),
*nothing else in your program* can happen for those 5 seconds. That's fine
for a simple script, but useless for a scheduler: if `scheduleJob()` had to
personally sit and wait for the due time before returning, you could never
schedule a second job while the first one is still waiting.

The fix: a **thread**. Think of a thread as a separate worker who can run
code *at the same time* as everyone else, all inside the same running
program, sharing the same memory (same objects, same variables). Your
`Main` method already runs on one thread (usually called "the main
thread"). This project creates two *more* kinds of threads:

1. **One dispatcher thread** (`JobSchedulerService` constructor, field
   `dispatcherThread`) — its only job, forever, is "wait for the next job
   that's due, then hand it off." It never runs a job itself.
2. **A small pool of worker threads** (`workerPool`, a fixed-size
   `ExecutorService`) — these are the ones that actually call
   `job.execute()`.

Splitting these two jobs apart matters: if the dispatcher itself ran jobs
directly, a single slow job would block it from ever noticing that the
*next* job became due. Keeping "notice what's due" and "actually run it"
on different threads means a slow job only blocks itself, not everyone
else.

**The catch with multiple threads:** since they share the same memory, two
threads can try to read/write the same variable at the *exact* same
instant, and get corrupted or surprising results — this is called a **race
condition**. A big chunk of this codebase's design (explained below) exists
purely to avoid race conditions, using data structures specifically built
to be safe when many threads touch them at once.

---

## 3. The one door you're allowed to knock on

`src/services/JobSchedulerService.java` is the **only** class anything
outside the package is meant to call. Everything else (`model`, `strategy`,
`observer`, `repository`, `exceptions`) is a helper it uses internally.

| Method | What it does |
|---|---|
| `new JobSchedulerService(workerThreads)` | Starts the dispatcher thread + worker pool |
| `scheduleJob(jobId, job, trigger)` | Register a job to run once or repeatedly |
| `cancelJob(jobId)` | Stop a job (best-effort — see §7) |
| `getJobStatus(jobId)` | Look up a job's current `JobStatus` |
| `addListener(listener)` | Get notified when jobs complete or fail |
| `shutdown()` | Stop the dispatcher and worker pool cleanly |

---

## 4. Read the code in this order

### Step 1 — the building blocks (`src/model/`)

- **`Job.java`** — one method, `execute()`. This is the **Command**
  pattern: a `Job` is just "a piece of work," with no idea when it will run
  or how often. That decision lives entirely somewhere else (`Trigger`,
  next).
- **`JobStatus.java`** — an enum: `SCHEDULED`, `RUNNING`, `COMPLETED`,
  `FAILED`, `CANCELLED`. Every `ScheduledTask` is in exactly one of these
  states at any moment.
- **`ScheduledTask.java`** — the most important class in this project.
  It bundles one `Job`, one `Trigger`, and the exact millisecond it's next
  due (`executionTimeMillis`). This is what actually sits inside the
  scheduler's queue — not the raw `Job`. It also implements a Java
  interface called `Delayed` (explained in §5).

**Nothing in `Job` or `JobStatus` decides anything by itself — they're
just data/behavior holders. `ScheduledTask` is the one class that carries
concurrency-safety logic (explained line by line in §7).**

### Step 2 — when should a job run again? (`src/strategy/`)

- **`Trigger.java`** — an interface with `firstExecutionTime(...)`,
  `nextExecutionTime(...)`, and `isRecurring()`.
- **`OneTimeTrigger.java`** — `firstExecutionTime` = "now + this many
  milliseconds"; `isRecurring()` is `false`, and `nextExecutionTime` is
  never even called for it.
- **`FixedRateTrigger.java`** — has *two* numbers: an initial delay (for
  the very first run) and an interval (for every run after that);
  `isRecurring()` is `true`.

This is the **Strategy** pattern: "how do I decide the next run time" is
completely swappable, and neither `Job` nor `JobSchedulerService` needs to
know which kind of trigger is in play — they just call the same three
methods on whatever `Trigger` object they were given.

### Step 3 — who gets told about outcomes? (`src/observer/`)

- **`JobListener.java`** — interface: `onJobCompleted(jobId)`,
  `onJobFailed(jobId, error)`.
- **`ConsoleJobListener.java`** — the one implementation shipped with the
  library; just prints to the console.

This is the **Observer** pattern. `JobSchedulerService` doesn't know or
care who's listening — it just loops over whatever listeners were
registered and calls both methods on each. `Main.java` proves the point by
registering *two* listeners at once: the shipped `ConsoleJobListener`
*and* its own private `TranscriptListener` (defined right inside
`Main.java`) that feeds the test transcript — neither listener knows the
other exists.

### Step 4 — where jobs are looked up by id (`src/repository/JobRegistry.java`)

A thin wrapper around a `ConcurrentHashMap<String, ScheduledTask>` (map
explained in §5). Three operations: `register(task)`, `contains(jobId)`,
`findByJobId(jobId)` (throws `JobNotFoundException` if missing). This
exists so cancellation/status-lookup by id is instant, instead of having
to search through every pending job one by one.

### Step 5 — errors (`src/exceptions/`)

- **`JobNotFoundException`** — `cancelJob`/`getJobStatus` on an unknown id.
- **`DuplicateJobIdException`** — `scheduleJob` reusing an id that's still
  in the registry (including *completed* one-time jobs — the registry
  never forgets, see the README's "Known gaps").

### Step 6 — the orchestrator (`src/services/JobSchedulerService.java`)

Everything from Steps 1–5 gets wired together here. Read §7 below for a
full line-by-line trace of what actually happens.

### Step 7 — the runner (`src/Main.java`)

Not part of the "real" library — a test harness. It reads a text script
line by line (`test/input/scenario.txt`), turns each line into a call on
`JobSchedulerService`, and records everything to
`test/output/output.txt`. It has its own tiny command language:

| Command | What it does |
|---|---|
| `SCHEDULE <id> <PRINT\|FAIL> <delayMs> [intervalMs]` | Schedules a job that either logs a line or throws, once or on an interval |
| `CANCEL <id>` | Calls `cancelJob` |
| `WAIT <ms>` | Sleeps the *main* thread only — background jobs keep running |
| `LOG` | Dumps every background event recorded since the last `LOG` |
| `STATUS <id>` | Prints the job's current `JobStatus` |
| `SHUTDOWN` | Calls `shutdown()` |

---

## 5. The data structures — explained with plain analogies before the code

### `DelayQueue<ScheduledTask>` — the heart of the whole system

**Java's built-in analogy: a smart pile of alarm clocks.** You can drop as
many alarm clocks into this pile as you want, each set for a different
time. One method, `take()`, means "give me the next clock to go off — and
if none are ringing yet, just wait right here until one is." Nobody has to
keep glancing at their watch (that would be "polling," and it wastes CPU
checking over and over). The thread that calls `take()` truly goes to
sleep and gets woken up by the JVM at exactly the right moment.

That's exactly what the dispatcher thread does, in
`JobSchedulerService.dispatchLoop()`:
```java
task = delayQueue.take();   // blocks here until something is due
```

For an object to go inside a `DelayQueue`, it must implement `Delayed`,
which is two methods — this is why `ScheduledTask` implements it:
- `getDelay(unit)` — "how much time is left before I'm due?" (can be
  negative, meaning "already due")
- `compareTo(other)` — "who's due first, me or you?" — needed so the queue
  can keep itself sorted internally (it's a **priority queue**: a data
  structure that keeps its smallest/soonest item quick to grab, without
  fully re-sorting everything every time something is added).

**The one rule that trips people up:** once a `ScheduledTask` is sitting
inside the queue, its due time (`executionTimeMillis`) must **never
change**. Mutating the value a priority queue is currently sorting by
corrupts its internal ordering (the same rule applies to Java's plain
`PriorityQueue`, which is what a parking-lot's spot-availability index
also uses — see `parking-lot/`). That's exactly why
`ScheduledTask.executionTimeMillis` is `final`, and why a recurring job's
*next* run is built as a **brand-new** `ScheduledTask` object
(`withNextExecution(...)`) instead of editing the one that just ran.

### `ConcurrentHashMap<String, ScheduledTask>` — inside `JobRegistry`

A normal `HashMap` is **not safe** if two threads read/write it at the same
time — you can get corrupted internal structure, lost entries, even
infinite loops in older Java versions. `ConcurrentHashMap` is a version of
the same "map from key to value" idea, but built from the ground up so
many threads can read and write it simultaneously without stepping on each
other, and *without* needing you to manually lock anything yourself. Think
of it as a filing cabinet with a smart lock built into each drawer,
instead of one lock on the whole cabinet — two people can each be filing
different drawers at once.

We need this because at least three different threads touch job records:
the thread that calls `scheduleJob`/`cancelJob` (often the main thread),
the dispatcher thread, and whichever worker thread just finished a
recurring job and is re-registering its next occurrence.

### `ExecutorService` (a fixed thread pool) — `workerPool`

Creating a brand-new thread for every single job is wasteful — threads
aren't free (each one costs real memory and OS bookkeeping). An
`ExecutorService` is a small, fixed-size **pool** of already-created
worker threads that sit around waiting for work. You call
`workerPool.submit(someTask)` and whichever worker is free next picks it
up. Think of it like a small team of two on-call workers (this project
uses `new JobSchedulerService(2)`) rather than hiring a brand-new person
for every single task and firing them the moment it's done.

### `AtomicReference<JobStatus>` and `AtomicBoolean` — inside `ScheduledTask`

Plain fields like `int` or a plain object reference are **not** safe to
update from one thread and read from another without extra care — a
second thread might see a half-written value, or an old cached copy. An
`AtomicReference`/`AtomicBoolean` is a small wrapper Java provides that
guarantees: (a) reads/writes are always fully visible across threads
immediately, and (b) it supports **compare-and-set (CAS)** — "only change
the value if it's currently what I expect it to be, and tell me whether
that succeeded." That's what makes `ScheduledTask.cancel()` race-safe:

```java
public boolean cancel() {
    cancelRequested.set(true);
    return status.compareAndSet(JobStatus.SCHEDULED, JobStatus.CANCELLED);
}
```

"Only flip me from `SCHEDULED` to `CANCELLED` if I am, at this exact
instant, still `SCHEDULED`." If the dispatcher had *already* called
`markRunning()` a microsecond earlier, the CAS simply fails and returns
`false` — no crash, no corrupted state, just an honest "too late, it's
already running." Without CAS, you'd have to write "read status, decide,
then write status" as three separate steps — and another thread could
sneak in a change between your read and your write. That gap is exactly
what a race condition is, and CAS closes it by making the check-and-swap
one indivisible step.

---

## 6. Order of operations — three complete traces

### Trace A — scheduling and running a one-time job

```
Main.java: "SCHEDULE job1 PRINT 100"
   |
   v
JobSchedulerService.scheduleJob("job1", job, new OneTimeTrigger(100))
   | executionTime = trigger.firstExecutionTime(now)   // now + 100ms
   | registry.register(task)                            // ConcurrentHashMap.put
   | delayQueue.put(task)                                // drop the "alarm clock" in the pile
   v
[scheduleJob returns immediately -- Main is free to run the NEXT script line right away]

  ... meanwhile, on the dispatcher thread (already running, in a loop) ...

delayQueue.take()  <-- was blocked, wakes up right at the 100ms mark
   | task.getStatus() == CANCELLED?  no
   | task.markRunning()                                  // AtomicReference.set(RUNNING)
   | workerPool.submit(() -> runTask(task))               // hand off, don't wait for it
   v
[dispatcher immediately loops back to delayQueue.take() for the NEXT due job]

  ... meanwhile, on a worker thread from the pool ...

runTask(task)
   | job.execute()                       // the actual work: appends "EXEC job1" to the transcript
   | task.markCompleted()
   | notifyCompleted("job1")             // loops over every registered JobListener
   |   -> ConsoleJobListener prints to console
   |   -> Main's TranscriptListener appends "COMPLETED job1"
   | trigger.isRecurring()?  false -> nothing more to do, task stays COMPLETED forever
```

Three different threads touched this one job (main, dispatcher, worker) —
and it all worked correctly with **zero manual locks written by us**,
because `DelayQueue` and `ConcurrentHashMap` already handle the unsafe
parts internally.

### Trace B — a recurring job reschedules itself

```
runTask(task) just finished job.execute() for job4 (a FixedRateTrigger job)
   | task.markCompleted(); notifyCompleted("job4")
   | task.getTrigger().isRecurring()        -> true
   | task.isCancelRequested()               -> false (nobody cancelled it)
   v
nextTime = trigger.nextExecutionTime(task.executionTimeMillis)   // previous + intervalMillis
nextTask = task.withNextExecution(nextTime)    // a FRESH ScheduledTask, new AtomicReference etc.
registry.register(nextTask)                    // same jobId key, NEW value -> old task object is now unreachable via lookup
delayQueue.put(nextTask)                       // drop a new "alarm clock" for the next tick
```

This repeats forever until either the trigger stops being recurring (it
never does, for `FixedRateTrigger`) or someone cancels the job.

### Trace C — cancelling a job that's still pending

```
Main.java: "CANCEL job4"
   v
JobSchedulerService.cancelJob("job4")
   | task = registry.findByJobId("job4")     // O(1) map lookup -> the CURRENT pending ScheduledTask
   | task.cancel()
   |    cancelRequested.set(true)
   |    status.compareAndSet(SCHEDULED, CANCELLED)   -> succeeds (it hadn't started running)
   v
return true    // "yes, I stopped it before it ran"

  ... later, when this task's delay actually expires ...

delayQueue.take()  -> returns this same task object
   | task.getStatus() == CANCELLED?  YES
   | continue;   // skip straight back to take() -- job.execute() is never called, and
                 // because runTask() never runs, there is no reschedule either -- the
                 // whole recurring chain quietly stops here.
```

Notice the queue itself is **never touched during cancellation** — no
searching, no removing from the middle of the heap. The task simply gets
popped at its normal time and thrown away unexecuted. This is called
**lazy cancellation**, and it's exactly what Java's own
`ScheduledThreadPoolExecutor` does internally — cheap and race-safe,
at the cost of the cancelled task sitting harmlessly in the queue a little
longer than strictly necessary.

---

## 7. Reading the actual captured run (`test/output/output.txt`)

A few real lines from the run, annotated:

```
> SCHEDULE job1 PRINT 100
OK scheduled job1 (one-time)
> WAIT 500
OK waited 500ms
> LOG
LOG
  [t+270ms] EXEC job1
  [t+286ms] COMPLETED job1
```

`job1` was scheduled with a 100ms delay, but the log shows it actually ran
at **t+270ms**, not t+100ms. That's not a bug — it's real-world thread
scheduling jitter: the OS/JVM don't guarantee *exactly* 100ms, only "no
earlier than 100ms." This is precisely why the test script always `WAIT`s
far longer (500ms) than any job's delay before checking the log — if it
only waited 100ms, the job might not have run yet and the test would be
**flaky** (sometimes pass, sometimes fail, depending on timing luck). This
is a real, general lesson for testing concurrent code: never assert
"exactly N milliseconds," always build in generous margin.

```
> SCHEDULE job4 PRINT 100 150
OK scheduled job4 (recurring)
> WAIT 600
OK waited 600ms
> LOG
LOG
  [t+1170ms] EXEC job4
  [t+1298ms] EXEC job4
  [t+1459ms] EXEC job4
  [t+1599ms] EXEC job4
> STATUS job4
STATUS job4 -> SCHEDULED
```

Four executions roughly ~130-160ms apart, matching the 150ms interval
(again, not exact — jitter). `STATUS job4` right after says `SCHEDULED`,
not `COMPLETED` — because by the time we asked, the 4th run had already
finished and rescheduled a brand-new *5th* `ScheduledTask`, which is
sitting in the queue waiting, hence `SCHEDULED`. This is Trace B happening
live.

```
> CANCEL job4
OK cancelled job4 before it ran
> WAIT 400
OK waited 400ms
> LOG
LOG
> STATUS job4
STATUS job4 -> CANCELLED
```

After cancelling, the next `WAIT 400` + `LOG` produces **no** `EXEC job4`
lines at all — proof the recurrence actually stopped (Trace C).

```
> SCHEDULE job1 PRINT 100
ERROR DuplicateJobIdException: A job is already registered with id: job1
```

`job1` finished minutes of script-time ago, but it's still sitting in
`JobRegistry` with status `COMPLETED` (nothing ever evicts it — see the
README's "Known gaps"), so reusing its id is correctly rejected.

---

## 8. Breakpoints worth setting while stepping through in VS Code

1. **`JobSchedulerService.dispatchLoop()`, the `task = delayQueue.take();`
   line.** Step through a full `SCHEDULE ... WAIT ...` pair from the test
   script and watch this line unblock the *instant* your delay elapses —
   confirms it's event-driven, not polling.
2. **`ScheduledTask.cancel()`.** Set a breakpoint here, run the `CANCEL
   job4` line, and inspect `status` before/after — watch the
   `compareAndSet` actually flip the `AtomicReference`'s internal value.
3. **`JobSchedulerService.runTask()`, the `if (task.getTrigger().isRecurring()
   ...)` line.** Step through one tick of `job4` and watch a *brand new*
   `ScheduledTask` object get created via `withNextExecution` — compare
   its object identity/hashcode to the one that just ran to convince
   yourself it's genuinely a different instance, not a mutation.
4. **`Main.execute()`, the `LOG` case.** Put a breakpoint on
   `transcript.poll()` and watch entries appear in whatever order the
   background worker threads actually finished in — a good way to *feel*
   that this ordering is determined by real thread timing, not by the
   order jobs were scheduled in.

---

## 9. Things to try / test yourself

Edit `test/input/scenario.txt` and re-run:
```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

1. **Make a recurring job that always fails.** `SCHEDULE x FAIL 100 150`,
   then `WAIT 500` + `LOG`. You should see repeated `FAILED x` lines — a
   failure does *not* stop future recurrences (see `runTask`: the
   reschedule check only looks at `isRecurring()`/`isCancelRequested()`,
   never at whether the last run succeeded).
2. **Cancel a job that's already finished.** `CANCEL job1` after it's
   already `COMPLETED`. You should get `NOOP ... already
   running/finished` — `cancel()`'s CAS fails because status is no longer
   `SCHEDULED`.
3. **Race a cancel against a very short delay.** Schedule a job with a
   1ms delay and immediately try to cancel it (no `WAIT` in between). Run
   it a few times — sometimes you'll get `OK cancelled ... before it ran`,
   sometimes `NOOP ... already running`, depending purely on which thread
   wins the race. Both outcomes are correct; this is what "best-effort
   cancellation" means in practice.
4. **Increase the worker pool to 1** (`new JobSchedulerService(1)` in
   `Main.java`) and schedule two one-time jobs due at nearly the same
   moment. Watch their `EXEC` lines appear one after another instead of
   overlapping — proof the pool size genuinely limits how much runs in
   parallel.
5. **Break something on purpose.** Try `STATUS ghost-job` (never
   scheduled) or `CANCEL ghost-job` — confirm you get
   `JobNotFoundException`, and trace it back through
   `JobRegistry.findByJobId` to see exactly where it's thrown.
