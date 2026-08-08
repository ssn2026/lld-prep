# -*- coding: utf-8 -*-
"""Regenerates producer-consumer.drawio from docs/tooling/drawio_uml.py.

Run from the repo root: python producer-consumer/diagrams/generate.py

Unlike the LLD problems (chess/, parking-lot/, ...), producer-consumer/ is
one curriculum spread across 5 stage folders rather than one class
hierarchy, so this is ONE consolidated diagrams/ folder at the
producer-consumer/ level (not per-stage) covering the key types across all
5 stages plus the 3 most illustrative sequence diagrams. Copied from
chess/diagrams/generate.py's structure per CLAUDE.md - only data lives
here, all escaping/geometry logic lives in the shared module.
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "docs", "tooling"))
from drawio_uml import (uml_box, group_title, edge, lifeline, msg, selfcall,
                         frame, divider, note, page, write_mxfile, validate)

# ===========================================================================
# PAGE 1: CLASS DIAGRAM (key types across all 5 stages)
# ===========================================================================
cells = []
COL = [40, 400, 760, 1120, 1480]
y = 20

cells.append(group_title(COL[0], y, "buffer — bounded-buffer implementations (stages 1, 3, 4)"))
y += 34
box, buffer_if_id, h1 = uml_box(COL[0], y, 280, "Buffer<T>", stereotype="interface (stage3 & stage4)",
    methods=["+ put(item): void", "+ take(): T"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, bb_id, h2 = uml_box(COL[1], y, 300, "BoundedBuffer<T>", stereotype="stage1 · synchronized+wait/notifyAll",
    attrs=["- queue: Queue<T>", "- capacity: int"],
    methods=["+ put(item): void", "+ take(): T"])
cells += box
box, safe_id, h3 = uml_box(COL[2], y, 300, "SafeBoundedBuffer<T>", stereotype="stage3 · notifyAll",
    attrs=["- queue: Queue<T>", "- capacity: int"],
    methods=["+ put(item): void", "+ take(): T"])
cells += box
box, unsafe_id, h4 = uml_box(COL[3], y, 300, "UnsafeBoundedBuffer<T>", stereotype="stage3 · notify (buggy on purpose)",
    attrs=["- queue: Queue<T>", "- capacity: int"],
    methods=["+ put(item): void", "+ take(): T"],
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, sem_id, h5 = uml_box(COL[4], y, 300, "SemaphoreBoundedBuffer<T>", stereotype="stage4 · Semaphore x2 + mutex",
    attrs=["- freeSlots/filledSlots: Semaphore", "- mutex: Lock"],
    methods=["+ put(item): void", "+ take(): T"])
cells += box
cells.append(edge(safe_id, buffer_if_id, "realize", exitX="0.2", exitY="0", entryX="0.6", entryY="1"))
cells.append(edge(unsafe_id, buffer_if_id, "realize", exitX="0.2", exitY="0", entryX="0.8", entryY="1"))
cells.append(edge(sem_id, buffer_if_id, "realize", exitX="0.1", exitY="0", entryX="1", entryY="1"))
row1_bottom = y + max(h1, h2, h3, h4, h5)

y = row1_bottom + 70
cells.append(group_title(COL[0], y, "buffer — stage 5: ring buffer, per-cell locks, broadcast + resettable offsets"))
y += 34
box, cell_id, hc1 = uml_box(COL[0], y, 300, "Cell<T>", stereotype="stage5 · own Lock+Condition per slot",
    attrs=["- lock: Lock", "- written: Condition", "- sequence: long", "- data: T"],
    methods=["  publish(seq, value): void", "  awaitSequence(expectedSeq): T"])
cells += box
box, ring_id, hc2 = uml_box(COL[1], y, 360, "RingBuffer<T>", stereotype="stage5 · CAS claim + per-cell locks + poll wait",
    attrs=["- cells: Cell<T>[]", "- nextSequence: AtomicLong", "- consumerOffsets: AtomicLong[]"],
    methods=["+ publish(value): long", "+ consume(consumerId): T", "+ getOffset(consumerId): long",
             "+ resetOffset(consumerId, newOffset): void",
             "- awaitOverwriteSafe(seq): void  // spin then LockSupport.parkNanos",
             "- minConsumerOffset(): long"])
cells += box
cells.append(edge(ring_id, cell_id, "composition", "cells  1..*", exitX="0.15", exitY="0.5", entryX="1", entryY="0.5"))
row2_bottom = y + max(hc1, hc2)

y = row2_bottom + 70
cells.append(group_title(COL[0], y, "worker — representative producer/consumer shapes per stage"))
y += 34
box, w1_id, hw1 = uml_box(COL[0], y, 300, "Producer / Consumer", stereotype="stage1 · plain Runnable, blocking put/take",
    methods=["  run(): void  // loop of buffer.put/take"])
cells += box
box, w2_id, hw2 = uml_box(COL[1], y, 320, "TaggedProducer / TaggedConsumer", stereotype="stage3 & stage4 · MPMC, ticket-based",
    attrs=["- ticketDispenser: AtomicInteger  // consumer side"],
    methods=["  run(): void  // tags items \"P{id}-{i}\" for verification"])
cells += box
box, w3_id, hw3 = uml_box(COL[2], y, 320, "RingProducer / RingConsumer", stereotype="stage5 · broadcast, own consumerId",
    methods=["  run(): void  // consumer reads its own offset totalItems times"])
cells += box
row3_bottom = y + max(hw1, hw2, hw3)

y = row3_bottom + 40
cells.append(note(COL[0], y, 1740,
    "Each stage folder compiles independently (its own src/ tree) - classes with the same name across\n"
    "stages (e.g. Buffer<T>) are separate compiled types, not shared code. See GUIDE.md for why each\n"
    "design exists and what problem it solves that the previous stage didn't.", h=54))

PAGE1 = "\n".join(cells)

# ===========================================================================
# PAGE 2: SEQUENCE — Stage 1 SPSC happy path (block on empty, wake on notifyAll)
# ===========================================================================
cells2 = []
xs = {}
for name, x in [("producerThread", 120), ("buffer: BoundedBuffer", 460), ("consumerThread", 820)]:
    box, xx = lifeline(x, name, bottom=620)
    cells2 += box
    xs[name] = xx

y = 120
cells2.append(msg(xs["consumerThread"], xs["buffer: BoundedBuffer"], y, "take()  // queue is empty"))
y += 40
cells2.append(selfcall(xs["buffer: BoundedBuffer"], y, "while(queue.isEmpty()) wait()", loop_w=140, loop_h=24))
y += 70
cells2.append(note(xs["buffer: BoundedBuffer"] - 60, y, 360, "consumerThread is now parked, lock released", h=30))
y += 60
cells2.append(msg(xs["producerThread"], xs["buffer: BoundedBuffer"], y, "put(item)  // queue not full"))
y += 40
cells2.append(selfcall(xs["buffer: BoundedBuffer"], y, "queue.add(item)", loop_w=90, loop_h=22))
y += 50
cells2.append(selfcall(xs["buffer: BoundedBuffer"], y, "notifyAll()", loop_w=80, loop_h=22))
y += 60
cells2.append(msg(xs["buffer: BoundedBuffer"], xs["consumerThread"], y, "wakes, re-acquires lock, rechecks while(isEmpty())", kind="return"))
y += 50
cells2.append(selfcall(xs["consumerThread"], y, "queue.poll() -> item", loop_w=90, loop_h=22))
y += 60
cells2.append(note(xs["producerThread"] - 40, y, 760,
    "Real interleaving varies run to run - this is ONE possible ordering, not a fixed transcript.\n"
    "See GUIDE.md Part 4 for why these demos verify invariants instead of exact output.", h=44))

PAGE2 = "\n".join(cells2)

# ===========================================================================
# PAGE 3: SEQUENCE — Stage 3, SAFE (notifyAll) vs UNSAFE (notify) contrast
# ===========================================================================
cells3 = []
xs = {}
for name, x in [("producerA", 100), ("consumerA (waiting, empty)", 420),
                 ("consumerB (waiting, empty)", 800), ("buffer", 1140)]:
    box, xx = lifeline(x, name, top=90, bottom=520)
    cells3 += box
    xs[name] = xx

cells3.append(frame(40, 1500, 40, 480, "SAFE: notifyAll() wakes every waiter; each rechecks its own condition"))
y = 150
cells3.append(selfcall(xs["consumerA (waiting, empty)"], y, "wait()  // parked", loop_w=70, loop_h=20))
cells3.append(selfcall(xs["consumerB (waiting, empty)"], y, "wait()  // parked", loop_w=70, loop_h=20))
y += 80
cells3.append(msg(xs["producerA"], xs["buffer"], y, "put(item) -> notifyAll()"))
y += 40
cells3.append(msg(xs["buffer"], xs["consumerA (waiting, empty)"], y, "wake, recheck -> not empty -> proceed", kind="return"))
cells3.append(msg(xs["buffer"], xs["consumerB (waiting, empty)"], y + 30, "wake, recheck -> still empty -> wait() again", kind="return"))
y += 90
cells3.append(note(60, y, 1440, "Result: exactly one consumer proceeds, the other correctly re-parks. No thread is stuck forever.", h=30))

y2 = 620
cells3b = []
for name, x in [("producerA (waiting, full)", 100), ("consumerA", 500), ("buffer2", 900)]:
    box, xx = lifeline(x, name, top=y2 + 40, bottom=y2 + 460)
    cells3b += box
    xs[name] = xx
cells3.extend(cells3b)
cells3.append(frame(40, 1100, y2, 480, "UNSAFE: notify() picks ONE arbitrary waiter, wrong kind or right - it can't tell"))
y = y2 + 140
cells3.append(selfcall(xs["producerA (waiting, full)"], y, "wait()  // parked, buffer was full", loop_w=70, loop_h=20))
y += 80
cells3.append(msg(xs["consumerA"], xs["buffer2"], y, "take() -> item removed -> notify()"))
y += 40
cells3.append(msg(xs["buffer2"], xs["producerA (waiting, full)"], y, "notify() happens to wake THIS thread (arbitrary choice)", kind="return"))
y += 50
cells3.append(note(60, y, 1000,
    "If some OTHER consumer was also parked waiting on \"not empty\" at this moment, it is never\n"
    "woken - nothing else will call notify() until another put()/take() happens. That consumer is a\n"
    "LOST WAKEUP: parked forever even though data may already be available. See Stage 3's stress\n"
    "test: 6/6 trials hung under real 3-producer/3-consumer contention.", h=58))

PAGE3 = "\n".join(cells3)

# ===========================================================================
# PAGE 4: SEQUENCE — Stage 5 publish / consume / resetOffset
# ===========================================================================
cells4 = []
xs = {}
for name, x in [("producerThread", 100), ("ring: RingBuffer", 420), ("cell: Cell", 740),
                 ("consumerThread", 1060)]:
    box, xx = lifeline(x, name, bottom=980)
    cells4 += box
    xs[name] = xx

y = 120
cells4.append(msg(xs["producerThread"], xs["ring: RingBuffer"], y, "publish(value)"))
y += 40
cells4.append(selfcall(xs["ring: RingBuffer"], y, "seq = nextSequence.getAndIncrement()  // lock-free CAS", loop_w=140, loop_h=22))
y += 60
cells4.append(selfcall(xs["ring: RingBuffer"], y, "awaitOverwriteSafe(seq)  // spin, then parkNanos poll", loop_w=150, loop_h=22))
y += 70
cells4.append(msg(xs["ring: RingBuffer"], xs["cell: Cell"], y, "publish(seq, value)  // locks ONLY this cell"))
y += 40
cells4.append(selfcall(xs["cell: Cell"], y, "data=value; sequence=seq; written.signalAll()", loop_w=160, loop_h=22))
y += 70
cells4.append(msg(xs["ring: RingBuffer"], xs["producerThread"], y, "return seq", kind="return"))
y += 60

cells4.append(msg(xs["consumerThread"], xs["ring: RingBuffer"], y, "consume(consumerId)"))
y += 40
cells4.append(msg(xs["ring: RingBuffer"], xs["cell: Cell"], y, "awaitSequence(offset)"))
y += 40
cells4.append(selfcall(xs["cell: Cell"], y, "while(sequence != offset) written.await()", loop_w=150, loop_h=22))
y += 70
cells4.append(msg(xs["cell: Cell"], xs["ring: RingBuffer"], y, "return data", kind="return"))
y += 40
cells4.append(selfcall(xs["ring: RingBuffer"], y, "consumerOffsets[consumerId].incrementAndGet()", loop_w=170, loop_h=22))
y += 60
cells4.append(msg(xs["ring: RingBuffer"], xs["consumerThread"], y, "return value", kind="return"))
y += 60

cells4.append(frame(xs["consumerThread"] - 220, 460, y, 130, "resetOffset - Kafka-style seek, bounded to the retained window"))
y += 34
cells4.append(msg(xs["consumerThread"], xs["ring: RingBuffer"], y, "resetOffset(consumerId, newOffset)"))
y += 40
cells4.append(selfcall(xs["ring: RingBuffer"], y, "validate: floor <= newOffset <= writeSeq, else throw", loop_w=170, loop_h=22))
y += 70
cells4.append(msg(xs["ring: RingBuffer"], xs["consumerThread"], y, "consumerOffsets[consumerId].set(newOffset)", kind="return"))

PAGE4 = "\n".join(cells4)

# ===========================================================================
outpath = os.path.join(os.path.dirname(__file__), "producer-consumer.drawio")
write_mxfile([
    page("classDiagram", "1 - Class Diagram (all stages)", PAGE1, w=1900, h=1500),
    page("seqStage1", "2 - Sequence - Stage 1 SPSC", PAGE2, w=1300, h=750),
    page("seqStage3", "3 - Sequence - Stage 3 Safe vs Unsafe", PAGE3, w=1600, h=1150),
    page("seqStage5", "4 - Sequence - Stage 5 Ring Buffer", PAGE4, w=1500, h=1050),
], outpath)
validate(outpath)
print("wrote", outpath)
