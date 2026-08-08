package buffer;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * One slot in the ring. Its lock and condition belong to THIS cell only -
 * writing/reading cell i never blocks on whatever cell j is doing, which is
 * the entire point of Stage 5 versus Stage 3/4's one-lock-for-the-whole-
 * buffer designs.
 *
 * `sequence` doubles as both the slot's version number and its "is data for
 * offset X ready yet" flag: a cell starts at -1 (never written), and after
 * the producer that owns global sequence number S publishes here, sequence
 * becomes S. A consumer wanting offset S just waits until this cell's
 * sequence == S - it can't be fooled by a stale value from S - capacity
 * laps ago because that would be a *different* number.
 */
final class Cell<T> {

    private final Lock lock = new ReentrantLock();
    private final Condition written = lock.newCondition();
    private long sequence = -1;
    private T data;

    void publish(long seq, T value) {
        lock.lock();
        try {
            data = value;
            sequence = seq;
            written.signalAll();
        } finally {
            lock.unlock();
        }
    }

    T awaitSequence(long expectedSeq) throws InterruptedException {
        lock.lock();
        try {
            while (sequence != expectedSeq) {
                written.await();
            }
            return data;
        } finally {
            lock.unlock();
        }
    }
}
