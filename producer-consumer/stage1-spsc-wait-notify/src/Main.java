import buffer.BoundedBuffer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import worker.Consumer;
import worker.Producer;

/**
 * Stage 1: one producer thread, one consumer thread, one bounded buffer
 * guarded by a single intrinsic lock (see buffer.BoundedBuffer).
 *
 * Thread interleaving is inherently non-deterministic, so this doesn't
 * assert an exact transcript the way the LLD problems' Main classes do.
 * Instead it asserts the properties concurrency bugs actually violate:
 * every produced item is consumed exactly once, and - because this is
 * SPSC with a FIFO queue - in the exact order it was produced.
 */
public class Main {

    private static final int ITEM_COUNT = 30;
    private static final int BUFFER_CAPACITY = 5;

    public static void main(String[] args) throws IOException, InterruptedException {
        StringBuilder output = new StringBuilder();

        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(BUFFER_CAPACITY);
        List<String> eventLog = Collections.synchronizedList(new ArrayList<>());
        List<Integer> consumed = new ArrayList<>();

        Thread producerThread = new Thread(new Producer(buffer, 0, ITEM_COUNT, eventLog), "producer");
        Thread consumerThread = new Thread(new Consumer(buffer, ITEM_COUNT, consumed, eventLog), "consumer");

        long startNanos = System.nanoTime();
        producerThread.start();
        consumerThread.start();
        producerThread.join();
        consumerThread.join();
        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

        log(output, "Buffer capacity: " + BUFFER_CAPACITY + ", items: " + ITEM_COUNT);
        log(output, "Total events logged: " + eventLog.size() + " (expected " + (ITEM_COUNT * 2) + ")");
        log(output, "Items consumed: " + consumed.size() + " (expected " + ITEM_COUNT + ")");

        boolean countOk = consumed.size() == ITEM_COUNT;
        boolean orderOk = true;
        for (int i = 0; i < consumed.size(); i++) {
            if (consumed.get(i) != i) {
                orderOk = false;
                log(output, "ORDER MISMATCH at index " + i + ": expected " + i + " got " + consumed.get(i));
                break;
            }
        }
        log(output, "Exact-order check (SPSC + FIFO buffer must preserve order): " + (orderOk ? "PASS" : "FAIL"));
        log(output, "Count check (no item lost or duplicated): " + (countOk ? "PASS" : "FAIL"));
        log(output, "Elapsed: " + elapsedMillis + "ms");
        log(output, countOk && orderOk ? "OVERALL: PASS" : "OVERALL: FAIL");

        log(output, "");
        log(output, "First 10 interleaved events (order varies run to run, that's expected):");
        synchronized (eventLog) {
            for (int i = 0; i < Math.min(10, eventLog.size()); i++) {
                log(output, "  " + eventLog.get(i));
            }
        }

        if (args.length >= 1) {
            Files.writeString(Path.of(args[0]), output.toString());
        }
    }

    private static void log(StringBuilder output, String line) {
        System.out.println(line);
        output.append(line).append(System.lineSeparator());
    }
}
