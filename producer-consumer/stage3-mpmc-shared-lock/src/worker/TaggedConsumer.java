package worker;

import buffer.Buffer;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Work-queue semantics: each item is consumed by exactly ONE consumer.
 * Consumers race for "tickets" out of a shared counter (totalItems tickets
 * exist in total, however many consumer threads there are) so the total
 * number of take() calls across every consumer thread combined is exactly
 * totalItems - no more, no less - regardless of how the race resolves.
 */
public class TaggedConsumer implements Runnable {

    private final Buffer<String> buffer;
    private final AtomicInteger ticketDispenser;
    private final int totalItems;
    private final Collection<String> consumedLog;

    public TaggedConsumer(Buffer<String> buffer, AtomicInteger ticketDispenser, int totalItems, Collection<String> consumedLog) {
        this.buffer = buffer;
        this.ticketDispenser = ticketDispenser;
        this.totalItems = totalItems;
        this.consumedLog = consumedLog;
    }

    @Override
    public void run() {
        while (true) {
            int ticket = ticketDispenser.getAndIncrement();
            if (ticket >= totalItems) {
                return;
            }
            try {
                String item = buffer.take();
                consumedLog.add(item);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
