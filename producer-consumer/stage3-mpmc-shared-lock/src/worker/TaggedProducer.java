package worker;

import buffer.Buffer;
import java.util.Collection;

/** Tags each item with its origin ("P{producerId}-{i}") so the demo can verify no loss/duplication across N producers. */
public class TaggedProducer implements Runnable {

    private final Buffer<String> buffer;
    private final int producerId;
    private final int count;
    private final Collection<String> producedLog;

    public TaggedProducer(Buffer<String> buffer, int producerId, int count, Collection<String> producedLog) {
        this.buffer = buffer;
        this.producerId = producerId;
        this.count = count;
        this.producedLog = producedLog;
    }

    @Override
    public void run() {
        for (int i = 0; i < count; i++) {
            String item = "P" + producerId + "-" + i;
            try {
                buffer.put(item);
                producedLog.add(item);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
