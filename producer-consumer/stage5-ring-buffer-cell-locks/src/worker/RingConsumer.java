package worker;

import buffer.RingBuffer;
import java.util.List;

/** Broadcast semantics: each registered consumer reads every published item, in order, at its own pace. */
public class RingConsumer implements Runnable {

    private final RingBuffer<String> ring;
    private final int consumerId;
    private final int totalItems;
    private final List<String> consumedLog;

    public RingConsumer(RingBuffer<String> ring, int consumerId, int totalItems, List<String> consumedLog) {
        this.ring = ring;
        this.consumerId = consumerId;
        this.totalItems = totalItems;
        this.consumedLog = consumedLog;
    }

    @Override
    public void run() {
        for (int i = 0; i < totalItems; i++) {
            try {
                consumedLog.add(ring.consume(consumerId));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
