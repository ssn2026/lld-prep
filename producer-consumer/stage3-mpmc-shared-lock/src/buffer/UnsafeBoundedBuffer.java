package buffer;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Deliberately buggy sibling of SafeBoundedBuffer: identical except it
 * calls notify() instead of notifyAll(). With exactly one producer and one
 * consumer (Stage 1) this is indistinguishable from notifyAll() - there's
 * only ever one kind of waiter parked at a time, so waking "one arbitrary
 * waiter" and waking "everyone" are the same thing.
 *
 * Once there are multiple producers AND multiple consumers, the monitor's
 * wait set can hold a MIX of producers (blocked because the buffer was
 * full) and consumers (blocked because it was empty) at the same time.
 * notify() picks an arbitrary thread from that set with no idea which
 * predicate it's waiting on. If it happens to keep picking the wrong kind
 * of waiter - one whose while-condition is still false - that thread goes
 * straight back to wait() having accomplished nothing, and the thread that
 * actually COULD make progress may never get chosen. That's a lost wakeup:
 * a thread parked forever even though the condition it's waiting for is
 * now true, because nothing told it to look again.
 *
 * This does NOT reproduce on every run - it depends on the JVM's arbitrary
 * choice of which parked thread to wake, which is exactly what makes this
 * class of bug so dangerous: it can pass a light test suite and then hang
 * in production under real contention. See Main's stress-test section for
 * an honest (not cherry-picked) attempt to reproduce it.
 */
public class UnsafeBoundedBuffer<T> implements Buffer<T> {

    private final Queue<T> queue = new LinkedList<>();
    private final int capacity;

    public UnsafeBoundedBuffer(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void put(T item) throws InterruptedException {
        while (queue.size() == capacity) {
            wait();
        }
        queue.add(item);
        notify();
    }

    public synchronized T take() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();
        }
        T item = queue.poll();
        notify();
        return item;
    }
}
