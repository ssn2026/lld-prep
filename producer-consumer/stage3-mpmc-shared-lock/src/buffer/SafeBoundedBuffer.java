package buffer;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Stage 1's BoundedBuffer, unmodified in substance - the while-loop +
 * notifyAll() design already generalizes correctly to N producers / M
 * consumers with zero code changes. That's the point of this stage: prove
 * it under real multi-threaded load, and contrast it against
 * UnsafeBoundedBuffer (notify() instead of notifyAll()) to show *why*
 * "wake everyone, let each recheck" is the safe default once there's more
 * than one kind of waiter sharing a monitor.
 */
public class SafeBoundedBuffer<T> implements Buffer<T> {

    private final Queue<T> queue = new LinkedList<>();
    private final int capacity;

    public SafeBoundedBuffer(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void put(T item) throws InterruptedException {
        while (queue.size() == capacity) {
            wait();
        }
        queue.add(item);
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
}
