# Multithreading & the Producer-Consumer Problem — a study guide

> **Mode: Learning** — Claude-authored curriculum and code, not one of the
> design-pattern LLD problems tracked in `docs/TRACKER.md`. This is the
> durable reference; `README.md` in this folder is just a short index
> pointing here and at each stage's runnable code.

This guide exists because the producer-consumer problem is the smallest
vehicle that forces you to learn essentially every core multithreading
concept: races, critical sections, blocking coordination, the difference
between "correct" and "correct under contention," and eventually
fine-grained/lock-free design. Read part 1 once; treat the rest as
reference material to revisit stage by stage.

See [`diagrams/producer-consumer.drawio`](diagrams/producer-consumer.drawio)
for a visual class diagram of the key types across all 5 stages, plus
sequence diagrams for Stage 1's block/wake happy path, Stage 3's safe-vs-unsafe
`notify` contrast, and Stage 5's publish/consume/`resetOffset` flow.

---

## Part 1 — Foundations (read this once, before Stage 1)

### Threads and shared memory

A thread is an independent sequence of execution that shares the same heap
memory as every other thread in the process. That sharing is *the* reason
multithreading is hard: two threads can read and write the same object at
the same time, and neither the language nor the hardware guarantees
anything about how those operations interleave unless you tell it to.

### Race condition & critical section

A **race condition** is any situation where the correctness of a result
depends on the timing/interleaving of multiple threads. A **critical
section** is a piece of code that must not be executed by more than one
thread at a time because it touches shared mutable state in a way that
isn't safe to interleave (e.g. `queue.add(x)` — a linked list's internal
pointers get corrupted if two threads splice a node in "at the same time").

Not every race is a bug (plenty of code is deliberately racy-but-safe,
e.g. two threads incrementing independent counters) — the term specifically
flags *unsynchronized access to shared mutable state*.

### The Java Memory Model, in one paragraph

Without synchronization, the JIT and CPU are free to reorder instructions
and cache values in per-core registers — a write one thread makes is **not
guaranteed to ever become visible** to another thread, independent of
whether it corrupts anything. Entering/exiting a `synchronized` block,
acquiring/releasing a `Lock`, or reading/writing a `volatile` field all
establish a **happens-before** edge: everything the writer did before
releasing becomes visible to whoever acquires next. This is *why*
`synchronized`/`Lock`/`volatile` matter even for code that "looks" safe
from corruption — without them, visibility itself isn't guaranteed.

### Intrinsic locks (`synchronized`)

Every Java object carries one implicit lock (its **monitor**).
`synchronized void put(...) { ... }` is sugar for "acquire `this`'s lock,
run the body, release it even if an exception is thrown." Only one thread
can hold a given object's monitor at a time — that's how it enforces a
critical section.

### `wait()` / `notify()` / `notifyAll()`

Busy-waiting (`while (isFull()) {}`) burns a CPU core doing nothing.
`wait()` (only callable while holding the monitor) atomically releases the
lock and parks the thread; `notify()`/`notifyAll()` wake parked thread(s) so
they can re-acquire the lock and re-check.

**Always call `wait()` inside a `while`, never an `if`:**
1. **Spurious wakeups** — the JLS permits `wait()` to return with no
   corresponding `notify()` at all.
2. **`notifyAll()` wakes every waiter**, not just the one whose condition
   became true — each one must re-validate its own condition.
3. Even a targeted `notify()` only guarantees *someone* was chosen, not
   that the specific thread woken is the one whose condition is now true
   (see Stage 3).

### `ReentrantLock` + `Condition`

`java.util.concurrent.locks.ReentrantLock` is an explicit alternative to
`synchronized` — same mutual-exclusion idea, but you call `lock()`/`unlock()`
yourself (almost always in a `try`/`finally`), and you can carve out
**multiple named `Condition`s from one lock** via `lock.newCondition()`.
This is the key upgrade over intrinsic locks: instead of one shared "wake
everyone, let them recheck" queue, you can have a `notFull` condition and a
`notEmpty` condition that are woken independently — `signal()` on the right
one only ever wakes a thread that could actually be waiting on the right
predicate. `ArrayBlockingQueue` (Stage 2) is built exactly this way
internally.

### `Semaphore`

A counting permit: `acquire()` blocks until a permit is available and
decrements the count; `release()` increments it. Unlike a lock, a semaphore
has no concept of an "owner" — any thread can release. Two semaphores
(counting free slots and filled slots) plus a short mutex is a classic,
simple way to build a bounded buffer with zero risk of the
`notify()`-vs-`notifyAll()` hazard, because each semaphore only ever has
one *kind* of waiter (Stage 4).

### Atomics & compare-and-swap (CAS)

`AtomicInteger`/`AtomicLong`/`AtomicReference` wrap a `compareAndSet(expected,
new)` hardware instruction: "if the current value is still `expected`,
replace it with `new`, atomically, otherwise fail and let the caller
retry." `getAndIncrement()` is a CAS retry loop under the hood. This gives
you an atomic, **lock-free** update to a single value — no thread ever
blocks another to increment a shared counter. Stage 5's producers claim a
unique slot via `AtomicLong.getAndIncrement()` with zero locking at all.

### Deadlock, livelock, starvation — the failure modes to name-check

- **Deadlock**: thread A holds lock 1 and wants lock 2; thread B holds lock
  2 and wants lock 1. Neither ever proceeds. Classic prevention: always
  acquire multiple locks in a fixed global order.
- **Livelock**: threads keep changing state in response to each other but
  never make progress (e.g. both back off and retry in lockstep forever).
- **Starvation**: a thread never gets scheduled/never wins a race for a
  resource, even though the system overall is making progress.
- **Lost wakeup**: a thread is parked waiting for a condition that becomes
  true, but nothing ever wakes it to re-check (Stage 3's `notify()` bug).

### False sharing (why *per-cell* locks, not just "more locks")

CPU caches move data in fixed-size lines (typically 64 bytes). If two
unrelated `AtomicLong`s (or two `Cell` objects' lock state) happen to sit on
the same cache line, one core writing to *its* variable invalidates the
*other* core's cached copy of the whole line — even though the two
variables are logically independent. This is why real high-performance
ring buffers (LMAX Disruptor) pad their sequence counters to a full cache
line each. Stage 5's `Cell` array doesn't pad (that's a deliberate
simplification for readability — flagged in Stage 5's README), but the
*reason* per-cell locking beats one global lock is the same underlying
idea: minimize how much unrelated state contends for the same cache/lock.

### The producer-consumer problem itself

One or more **producer** threads generate items; one or more **consumer**
threads process them; a **bounded buffer** sits between them so producers
don't outrun memory (backpressure) and consumers don't spin on emptiness.
It's the smallest problem that requires *both* mutual exclusion (don't
corrupt the buffer) *and* condition synchronization (block when full/empty,
wake up when that changes) — which is why it's the standard vehicle for
teaching everything above.

---

## Part 2 — Stage by stage

Each stage's own `README.md`-equivalent lives as doc-comments in its code
(no separate per-stage README — this guide plus the code *is* the
documentation, kept in one place instead of scattered). All demos assert
correctness invariants rather than exact transcripts, because thread
interleaving is inherently non-deterministic — see "Testing concurrent
code" below for why.

### Stage 1 — SPSC, `wait()`/`notifyAll()`

`stage1-spsc-wait-notify/src/buffer/BoundedBuffer.java`

One producer, one consumer, one intrinsic lock. Textbook bounded buffer:
`synchronized` + `while (full) wait()` / `while (empty) wait()` +
`notifyAll()` after every state change.

**Verified**: 3 runs, `Count check: PASS`, `Exact-order check: PASS` every
time — order is *guaranteed* here specifically because it's 1:1 over a FIFO
queue (this guarantee is gone by Stage 3).

**Breakpoint**: `BoundedBuffer.put`/`take`, the `wait()` lines — watch one
thread park while the other holds the lock, then watch `notifyAll()` wake
it.

### Stage 2 — SPSC, `ArrayBlockingQueue`

`stage2-spsc-blockingqueue/src/Main.java`

Same shape, swapped for the JDK's built-in. Internally it's a
`ReentrantLock` with two `Condition`s (`notFull`, `notEmpty`) instead of one
shared monitor condition — worth opening the JDK source for
`ArrayBlockingQueue` once, to see `Condition`-per-predicate in the wild
before Stage 4 builds a version of that idea by hand.

**Verified**: `Count check: PASS`, `Exact-order check: PASS`.

### Stage 3 — MPMC, shared lock, and *why* `notify()` alone is unsafe

`stage3-mpmc-shared-lock/src/buffer/SafeBoundedBuffer.java` (notifyAll) vs.
`UnsafeBoundedBuffer.java` (notify)

Stage 1's design generalizes to N producers / M consumers with **zero code
changes** — that's proven here under real load (4 producers × 4 consumers,
60 tagged items, verified via set-equality + no-duplicates rather than
exact order, since order across multiple producers isn't well-defined
anymore).

The `Unsafe` sibling swaps `notifyAll()` for `notify()`. With only one kind
of waiter (Stage 1) that's indistinguishable from `notifyAll()`. With
producers *and* consumers sharing one wait set, `notify()` picks an
arbitrary thread with no idea whether it's the "right kind" — if it keeps
picking a thread whose condition is still false, the thread whose condition
*did* become true can be starved indefinitely: a **lost wakeup**.

**Verified** (this is the important part — not asserted, *observed*):

```
Experiment 1 (Safe, notifyAll): produced 60/60, consumed 60/60, no
duplicates, sets match. OVERALL: PASS.

Experiment 2 (Unsafe, notify), capacity=1, 3 producers x 3 consumers,
6 stress trials, 1.5s timeout each:
  trial 1: DID NOT COMPLETE (produced=9/30, consumed=9/30)
  trial 2: DID NOT COMPLETE (produced=9/30, consumed=9/30)
  trial 3: DID NOT COMPLETE (produced=6/30, consumed=6/30)
  trial 4: DID NOT COMPLETE (produced=8/30, consumed=8/30)
  trial 5: DID NOT COMPLETE (produced=22/30, consumed=21/30)
  trial 6: DID NOT COMPLETE (produced=4/30, consumed=4/30)
Hung on 6/6 trials.
```

That's not cherry-picked — it hung *every* trial at capacity 1 with 3:3
threads on this machine. The number will vary by JVM/OS/core count, and
could occasionally complete on a different run — **that unpredictability is
the actual lesson**: this bug can pass a light test suite and then hang in
production under real contention. Never rely on `notify()` unless you can
prove every waiter on that monitor is checking the *same* predicate.

**Breakpoint**: put a breakpoint on `UnsafeBoundedBuffer.take()`'s
`notify()` line with the debugger's thread view open — watch which thread
actually gets woken versus which threads are still parked.

### Stage 4 — MPMC, `Semaphore` + short mutex

`stage4-mpmc-semaphore/src/buffer/SemaphoreBoundedBuffer.java`

Sidesteps Stage 3's hazard *by construction*: `freeSlots` only ever has
producers waiting on it, `filledSlots` only ever has consumers. There's no
shared wait set for a wakeup to misfire on. The mutex (`ReentrantLock`) is
only held for the actual enqueue/dequeue — much shorter than Stage 1/3's
whole-method `synchronized`.

**Verified**: 4×4 threads, 60 items — `PASS` across all three invariants
(no duplicates, no loss, sets match).

### Stage 5 — the advanced target: ring buffer, per-cell locks, resettable offsets

`stage5-ring-buffer-cell-locks/src/buffer/{Cell,RingBuffer}.java`

This is a different *semantic model*, not just a faster lock: **broadcast**
instead of work-queue. Every registered consumer independently reads every
published item at its own pace, tracked by its own offset — like a Kafka
partition read by several consumer groups, or an LMAX Disruptor
`RingBuffer`. That's the only model where "reset a consumer's offset" is
even a meaningful operation (in a work-queue model, once an item is
consumed it's *gone* — there's nothing to seek back to).

Three distinct coordination mechanisms, each doing the minimum necessary:

1. **Claiming a slot to write — lock-free.**
   `nextSequence.getAndIncrement()` (an `AtomicLong` CAS loop). Two
   producers claiming concurrently get two different, gap-free sequence
   numbers, no blocking.
2. **Writing/reading one slot's data — per-cell lock.**
   Each `Cell` has its own `ReentrantLock`+`Condition`. A producer writing
   cell 3 and a consumer reading cell 7 run in true parallel — nothing
   about Stage 5's design serializes them, unlike every earlier stage's
   single buffer-wide lock.
3. **"Is it safe to overwrite this slot yet" — lock-free poll.**
   Before a producer can reuse a slot, the *slowest* of all registered
   consumers must have already read what's currently there — otherwise it
   would stomp on data an independent, possibly-paused reader hasn't
   gotten to. That fact depends on *every* consumer's offset, not one
   cell's state, so it can't be a single cell's `Condition` — instead
   `RingBuffer.awaitOverwriteSafe` spins briefly then polls via
   `LockSupport.parkNanos`. This is a real, named technique (compare
   Disruptor's `BusySpinWaitStrategy`/`SleepingWaitStrategy`), not a
   shortcut — and it's explicitly the one place this design trades a
   little latency to avoid a much more complex cross-cell signaling
   scheme.

`resetOffset(consumerId, newOffset)` mirrors Kafka's `seek()`: legal only
within `[max(0, writeSeq - capacity), writeSeq]` — the window the ring can
still physically prove it hasn't overwritten.

**Verified** (3 runs, capacity 10, 60 items — smaller than the item count,
so the ring genuinely wraps multiple times mid-run):

```
Consumer 0/1/2 each read 60/60 items
All consumers saw an identical, ordered stream: PASS
No item lost, duplicated, or fabricated: PASS
CORE OVERALL: PASS

Rewind consumer 0 to offset 52 (legal, inside [50, 60]) -> re-read matches
original tail exactly: PASS
Seek to offset 45 (outside the window) -> correctly rejected: PASS
```

**Breakpoints**:
- `RingBuffer.awaitOverwriteSafe` — pause with capacity small relative to
  producer speed and watch a producer thread actually spin/park waiting for
  `minConsumerOffset()` to advance.
- `Cell.publish`/`awaitSequence` — confirm two threads touching *different*
  cells never block on each other (breakpoint both, resume freely, notice
  neither one's stack ever shows the other cell's lock).
- `RingBuffer.resetOffset` — step through the boundary check with the
  illegal-seek call from the demo.

---

## Part 3 — Real-world analogs

| This curriculum | Real system that works the same way |
|---|---|
| Stage 1/3's single monitor + wait/notify | `java.util.Timer`'s internal task queue |
| Stage 2/4's Condition-per-predicate | `java.util.concurrent.ArrayBlockingQueue` internals |
| Stage 5's atomic slot claim + per-cell coordination | LMAX Disruptor `RingBuffer` |
| Stage 5's independent, resettable consumer offsets | Kafka partition read by multiple consumer groups, `consumer.seek(offset)` |
| Stage 5's "slowest consumer gates eviction" | Disruptor's gating `Sequence` / Kafka retention |
| Stage 5's spin-then-park wait strategy | Disruptor's `BusySpinWaitStrategy` / `SleepingWaitStrategy` |

---

## Part 4 — Testing concurrent code (why every `Main` here looks different from the LLD problems')

The other problems in this repo (`chess/`, `parking-lot/`, ...) assert an
**exact transcript** — same command script in, byte-identical output every
time, because they're single-threaded. That approach doesn't work here:
thread interleaving is *legitimately* different from run to run, and a test
that expects one fixed interleaving is a test that's wrong on principle,
not just occasionally flaky.

Instead every stage's `Main` runs a **self-verifying demo**: produce a known
set of items, consume them, then assert the properties a concurrency bug
would actually violate —

- **no loss** (produced count == consumed count),
- **no duplication** (consumed set has no repeats),
- **no fabrication** (consumed set ⊆ produced set),
- and, only where the design actually guarantees it, **exact ordering**
  (Stage 1/2's single producer; Stage 5's "all consumers see an identical
  sequence").

Stage 3's unsafe-buffer stress test goes one step further: instead of
asserting a property, it runs several trials with a hang timeout and
**reports what actually happened**, honestly, including the fact that the
result is inherently non-deterministic. That's a deliberate choice — a
guide that claims a flaky bug "always reproduces" would be teaching the
wrong lesson.

For production-grade concurrency testing beyond what a demo script can
cover, look into `jcstress` (the OpenJDK Java Concurrency Stress harness) —
not used here, but worth knowing exists once you're past this curriculum.

---

## Glossary (quick reference)

| Term | One-line meaning |
|---|---|
| Race condition | Correctness depends on thread timing/interleaving |
| Critical section | Code that must run in only one thread at a time |
| Monitor / intrinsic lock | The implicit lock every Java object carries, used by `synchronized` |
| Happens-before | The visibility guarantee synchronization establishes between threads |
| Spurious wakeup | `wait()` returning with no matching `notify()` |
| Lost wakeup | A thread parked forever despite its condition becoming true |
| `Condition` | A named wait-queue carved out of a `ReentrantLock`, unlike one shared monitor queue |
| Semaphore | A counting permit; `acquire()`/`release()`, no notion of "owner" |
| CAS (compare-and-swap) | Atomic "replace only if unchanged" hardware primitive behind the `Atomic*` classes |
| Deadlock | Circular wait on locks; nobody proceeds |
| Livelock | Threads keep reacting to each other but make no progress |
| Starvation | A thread never gets its turn, even though the system overall progresses |
| False sharing | Unrelated variables on the same CPU cache line causing needless cache invalidation |
| Broadcast semantics | Every consumer reads every item independently (vs. work-queue: each item to exactly one consumer) |
| Gating sequence | The slowest reader's position, which producers must not overwrite past |
