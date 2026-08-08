package buffer;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Same bounded-buffer contract as Stage 1/3, but coordination is split
 * across two purpose-built primitives instead of one shared monitor:
 *
 *  - two Semaphores count "how many free slots" / "how many filled slots"
 *    as PERMITS, not a boolean condition. acquire() on the wrong semaphore
 *    is simply impossible - a producer only ever calls acquire() on
 *    freeSlots, a consumer only ever calls acquire() on filledSlots. There
 *    is no shared wait set with a mix of waiter "kinds" the way Stage 3's
 *    single monitor had, so there's nothing for a stray wakeup to pick the
 *    wrong one of - the notify()-vs-notifyAll() hazard doesn't exist here
 *    by construction, not by discipline.
 *  - a plain mutex Lock guards only the few lines that actually touch the
 *    shared queue. Producers/consumers block on their semaphore FIRST and
 *    only hold the mutex for the brief enqueue/dequeue itself, so the
 *    critical section is much shorter than Stage 1/3's whole-method
 *    synchronized block.
 *
 * This is close to how ArrayBlockingQueue is actually built internally
 * (it uses one ReentrantLock with two Conditions rather than two
 * Semaphores, but the shape - split "how many" from "who's holding the
 * queue right now" - is the same idea).
 */
public class SemaphoreBoundedBuffer<T> implements Buffer<T> {

    private final Queue<T> queue = new LinkedList<>();
    private final Semaphore freeSlots;
    private final Semaphore filledSlots = new Semaphore(0);
    private final Lock mutex = new ReentrantLock();

    public SemaphoreBoundedBuffer(int capacity) {
        this.freeSlots = new Semaphore(capacity);
    }

    @Override
    public void put(T item) throws InterruptedException {
        freeSlots.acquire();
        mutex.lock();
        try {
            queue.add(item);
        } finally {
            mutex.unlock();
        }
        filledSlots.release();
    }

    @Override
    public T take() throws InterruptedException {
        filledSlots.acquire();
        T item;
        mutex.lock();
        try {
            item = queue.poll();
        } finally {
            mutex.unlock();
        }
        freeSlots.release();
        return item;
    }
}
