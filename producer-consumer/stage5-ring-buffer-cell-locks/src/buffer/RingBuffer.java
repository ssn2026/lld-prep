package buffer;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Multi-producer, multi-consumer ring buffer with BROADCAST semantics -
 * every registered consumer independently reads every published item, at
 * its own pace, tracked by its own offset (like a Kafka partition read by
 * several independent consumer groups, or an LMAX Disruptor RingBuffer).
 * This is a different contract from Stage 3/4's work-queue buffers, where
 * each item goes to exactly ONE consumer - broadcast is what makes
 * "independent, resettable per-consumer offsets" a meaningful idea at all.
 *
 * Two coordination mechanisms, deliberately different in kind:
 *
 *  1. Claiming a slot to write into is lock-free: nextSequence.getAndIncrement()
 *     is a single CAS loop, not a lock. Two producers claiming concurrently
 *     get two different, gap-free sequence numbers with no blocking at all.
 *
 *  2. Actually writing/reading a slot's data is guarded by THAT SLOT's own
 *     Cell lock (see Cell.java) - producers/consumers touching different
 *     cells run fully in parallel, never contending with each other.
 *
 *  3. The one thing that can't be scoped to a single cell: before
 *     overwriting a slot, a producer must know the SLOWEST of all
 *     registered consumers has already read whatever used to be there
 *     (otherwise it would stomp on data an independent, possibly-paused
 *     reader hasn't gotten to yet - exactly the hazard broadcast semantics
 *     introduce that a single-reader queue never has to worry about). That
 *     "slowest reader" fact depends on every consumer's offset, not one
 *     cell, so it can't be a single cell's Condition to wait on. This
 *     implementation uses a lock-free poll loop (spin briefly, then
 *     LockSupport.parkNanos) instead - a real "named" strategy (compare
 *     Disruptor's BusySpinWaitStrategy / SleepingWaitStrategy), not a hack,
 *     but worth naming as the one place this design trades some latency
 *     for avoiding a much more complicated cross-cell signaling scheme.
 */
public class RingBuffer<T> {

    private final Cell<T>[] cells;
    private final int capacity;
    private final AtomicLong nextSequence = new AtomicLong(0);
    private final AtomicLong[] consumerOffsets;

    @SuppressWarnings("unchecked")
    public RingBuffer(int capacity, int consumerCount) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.cells = new Cell[capacity];
        for (int i = 0; i < capacity; i++) {
            cells[i] = new Cell<>();
        }
        this.consumerOffsets = new AtomicLong[consumerCount];
        for (int i = 0; i < consumerCount; i++) {
            consumerOffsets[i] = new AtomicLong(0);
        }
    }

    /** Returns the global sequence number this value was published at. */
    public long publish(T value) {
        long seq = nextSequence.getAndIncrement();
        awaitOverwriteSafe(seq);
        cells[indexOf(seq)].publish(seq, value);
        return seq;
    }

    public T consume(int consumerId) throws InterruptedException {
        long seq = consumerOffsets[consumerId].get();
        T value = cells[indexOf(seq)].awaitSequence(seq);
        consumerOffsets[consumerId].incrementAndGet();
        return value;
    }

    public long getOffset(int consumerId) {
        return consumerOffsets[consumerId].get();
    }

    /**
     * Seeks a consumer to an arbitrary offset, like Kafka's seek(). Only
     * legal within the still-retained window: not before the oldest
     * sequence this bounded ring could possibly still hold (older data has
     * physically been overwritten), and not past what's been published so
     * far.
     */
    public void resetOffset(int consumerId, long newOffset) {
        long writeSeq = nextSequence.get();
        long floor = Math.max(0, writeSeq - capacity);
        if (newOffset < floor || newOffset > writeSeq) {
            throw new IllegalArgumentException("offset " + newOffset + " is outside the retained window ["
                    + floor + ", " + writeSeq + "]");
        }
        consumerOffsets[consumerId].set(newOffset);
    }

    private void awaitOverwriteSafe(long seq) {
        long previousLapOccupant = seq - capacity;
        if (previousLapOccupant < 0) {
            return; // first lap around the ring, nothing to evict yet
        }
        int spins = 0;
        while (minConsumerOffset() <= previousLapOccupant) {
            if (spins++ < 100) {
                Thread.onSpinWait();
            } else {
                LockSupport.parkNanos(50_000); // 0.05ms poll once short spinning doesn't resolve it
            }
        }
    }

    private long minConsumerOffset() {
        if (consumerOffsets.length == 0) {
            return nextSequence.get(); // no registered readers - nothing gates eviction
        }
        long min = Long.MAX_VALUE;
        for (AtomicLong offset : consumerOffsets) {
            long value = offset.get();
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    private int indexOf(long seq) {
        return (int) (seq % capacity);
    }
}
