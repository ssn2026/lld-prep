# Producer-Consumer: a concurrency curriculum

> **Mode: Learning** — Claude-authored, staged exercise sequence, not one of
> the design-pattern LLD problems tracked in `docs/TRACKER.md`. This folder
> deliberately doesn't follow the `model/strategy/services/...` shape used
> elsewhere in this repo — there's no design pattern to hang classes off of
> here, the subject is concurrency primitives themselves.

**Start with [`GUIDE.md`](GUIDE.md)** — the full concept reference (Java
Memory Model, locks, `wait`/`notify`, semaphores, CAS/atomics, deadlock and
friends, false sharing) plus a stage-by-stage walkthrough with verified
results and suggested breakpoints. This README is just an index.

## Stages

| Stage | Folder | Adds | Core primitive |
|---|---|---|---|
| 1 | [`stage1-spsc-wait-notify/`](stage1-spsc-wait-notify/) | 1 producer, 1 consumer | `synchronized` + `wait()`/`notifyAll()` on a hand-rolled bounded buffer |
| 2 | [`stage2-spsc-blockingqueue/`](stage2-spsc-blockingqueue/) | same 1:1, JDK version | `ArrayBlockingQueue` |
| 3 | [`stage3-mpmc-shared-lock/`](stage3-mpmc-shared-lock/) | N producers, N consumers | same single-lock buffer, generalized + an honest stress-tested demo of why `notify()` alone is unsafe |
| 4 | [`stage4-mpmc-semaphore/`](stage4-mpmc-semaphore/) | N:N, less contention | `Semaphore` (slot counting) + a short-held `Lock` |
| 5 | [`stage5-ring-buffer-cell-locks/`](stage5-ring-buffer-cell-locks/) | **advanced target** | custom ring buffer: atomic per-slot claim (no queue-wide lock), per-cell locks, independent resettable consumer offsets (broadcast semantics, like a Kafka partition / LMAX Disruptor) |

Every stage's `Main` runs a **self-verifying demo** (produce N known items,
consume them, assert no loss/duplication/fabrication and, where the design
guarantees it, exact ordering) rather than a fixed text transcript — thread
interleaving isn't deterministic, so "did it stay correct under
concurrency" is what's worth asserting, not one fixed log. See GUIDE.md
part 4 for why.

## Running any stage

```
cd producer-consumer/<stage-folder>
javac -d out $(find src -name "*.java")
java -cp out Main test/output/output.txt
```

VS Code: open the stage folder itself as the workspace root, use the
"Run Main (demo)" launch config (requires the "Extension Pack for Java" by
Microsoft).

## Diagrams

[`diagrams/producer-consumer.drawio`](diagrams/producer-consumer.drawio) —
one consolidated set for the whole curriculum (this "problem" is 5 stage
folders, not one class hierarchy, so it gets one `diagrams/` here at the
top level rather than one per stage): a class diagram covering the key
types across all 5 stages, plus 3 sequence diagrams for the most
illustrative interactions — Stage 1's SPSC block/wake happy path, Stage 3's
safe (`notifyAll`) vs. unsafe (`notify`) contrast, and Stage 5's
publish/consume/`resetOffset` flow. Generated via
[`diagrams/generate.py`](diagrams/generate.py) and
`docs/tooling/drawio_uml.py`, per this repo's diagram convention.

## Results (all verified — see GUIDE.md for full transcripts and analysis)

| Stage | Result |
|---|---|
| 1 | PASS (3 runs) — exact order preserved (guaranteed for 1:1 FIFO) |
| 2 | PASS — JDK `ArrayBlockingQueue` baseline |
| 3 | Safe buffer: PASS (60/60, no loss/dup). Unsafe buffer: hung on 6/6 stress trials — a genuine, reproduced lost-wakeup bug, not just a claim |
| 4 | PASS (4×4 threads, 60 items, no loss/dup) |
| 5 | PASS (3 runs) — real ring wraparound, all consumers see an identical stream, legal rewind + illegal out-of-window seek both behave correctly |
