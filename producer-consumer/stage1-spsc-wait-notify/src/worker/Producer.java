package worker;

import buffer.BoundedBuffer;
import java.util.List;

public class Producer implements Runnable {

    private final BoundedBuffer<Integer> buffer;
    private final int startInclusive;
    private final int count;
    private final List<String> eventLog;

    public Producer(BoundedBuffer<Integer> buffer, int startInclusive, int count, List<String> eventLog) {
        this.buffer = buffer;
        this.startInclusive = startInclusive;
        this.count = count;
        this.eventLog = eventLog;
    }

    @Override
    public void run() {
        for (int i = 0; i < count; i++) {
            int item = startInclusive + i;
            try {
                buffer.put(item);
                synchronized (eventLog) {
                    eventLog.add("PRODUCE " + item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
