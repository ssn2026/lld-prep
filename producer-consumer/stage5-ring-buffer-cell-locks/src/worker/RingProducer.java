package worker;

import buffer.RingBuffer;
import java.util.Collection;

public class RingProducer implements Runnable {

    private final RingBuffer<String> ring;
    private final int producerId;
    private final int count;
    private final Collection<String> producedLog;

    public RingProducer(RingBuffer<String> ring, int producerId, int count, Collection<String> producedLog) {
        this.ring = ring;
        this.producerId = producerId;
        this.count = count;
        this.producedLog = producedLog;
    }

    @Override
    public void run() {
        for (int i = 0; i < count; i++) {
            String item = "P" + producerId + "-" + i;
            ring.publish(item);
            producedLog.add(item);
        }
    }
}
