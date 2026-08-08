package worker;

import buffer.BoundedBuffer;
import java.util.List;

public class Consumer implements Runnable {

    private final BoundedBuffer<Integer> buffer;
    private final int count;
    private final List<Integer> consumed;
    private final List<String> eventLog;

    public Consumer(BoundedBuffer<Integer> buffer, int count, List<Integer> consumed, List<String> eventLog) {
        this.buffer = buffer;
        this.count = count;
        this.consumed = consumed;
        this.eventLog = eventLog;
    }

    @Override
    public void run() {
        for (int i = 0; i < count; i++) {
            try {
                int item = buffer.take();
                consumed.add(item);
                synchronized (eventLog) {
                    eventLog.add("CONSUME " + item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
