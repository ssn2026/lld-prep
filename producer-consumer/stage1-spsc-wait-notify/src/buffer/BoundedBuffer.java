package buffer;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Classic bounded-buffer producer-consumer coordination. A single intrinsic
 * lock (this instance's monitor, acquired by the `synchronized` keyword)
 * guards the queue, and wait()/notifyAll() park/wake threads instead of
 * spin-polling a flag.
 *
 * wait() sits inside a `while` loop, not `if`, for two independent reasons:
 *   1. Spurious wakeups: the JVM is allowed to return from wait() even if
 *      nobody called notify() at all (a POSIX pthread quirk the JLS permits
 *      rather than forbids, so implementations don't pay to prevent it).
 *   2. notifyAll() wakes every waiter, not just the one whose condition
 *      actually became true. With a single shared queue, a thread woken by
 *      notifyAll() must re-check the condition itself before proceeding -
 *      the JVM only reschedules it to *try* re-acquiring the lock, it does
 *      not re-validate anything on its behalf.
 * Re-checking the condition after every wakeup is what the while-loop does.
 */
public class BoundedBuffer<T> {

    private final Queue<T> queue = new LinkedList<>();
    private final int capacity;

    public BoundedBuffer(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void put(T item) throws InterruptedException {
        while (queue.size() == capacity) {
            wait();
        }
        queue.add(item);
        // Wakes any thread parked in put() OR take() - notifyAll (not notify) is
        // used on principle even though stage 1 only ever has one of each
        // waiting; stage 3 explains why notify() alone would be unsafe once
        // there's more than one kind of waiter.
        notifyAll();
    }

    public synchronized T take() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();
        }
        T item = queue.poll();
        notifyAll();
        return item;
    }

    public synchronized int size() {
        return queue.size();
    }
}
