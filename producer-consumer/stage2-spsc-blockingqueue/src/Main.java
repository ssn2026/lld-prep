import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Stage 2: identical 1 producer / 1 consumer shape as Stage 1, but backed by
 * java.util.concurrent.ArrayBlockingQueue instead of the hand-rolled
 * BoundedBuffer. Internally ArrayBlockingQueue uses one ReentrantLock plus
 * TWO Condition objects (notEmpty, notFull) carved out of that lock, rather
 * than stage 1's single implicit monitor condition. That split is exactly
 * what lets it call signal() on the precise condition instead of
 * notifyAll() on everyone - a preview of the exact problem Stage 3/4 dig
 * into (waking only the relevant kind of waiter, safely).
 */
public class Main {

    private static final int ITEM_COUNT = 30;
    private static final int CAPACITY = 5;

    public static void main(String[] args) throws IOException, InterruptedException {
        StringBuilder output = new StringBuilder();
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(CAPACITY);
        List<Integer> consumed = new ArrayList<>();

        Thread producer = new Thread(() -> {
            for (int i = 0; i < ITEM_COUNT; i++) {
                try {
                    queue.put(i);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "producer");

        Thread consumer = new Thread(() -> {
            for (int i = 0; i < ITEM_COUNT; i++) {
                try {
                    consumed.add(queue.take());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "consumer");

        long startNanos = System.nanoTime();
        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

        log(output, "ArrayBlockingQueue capacity: " + CAPACITY + ", items: " + ITEM_COUNT);

        boolean countOk = consumed.size() == ITEM_COUNT;
        boolean orderOk = true;
        for (int i = 0; i < consumed.size(); i++) {
            if (consumed.get(i) != i) {
                orderOk = false;
                log(output, "ORDER MISMATCH at index " + i + ": expected " + i + " got " + consumed.get(i));
                break;
            }
        }
        log(output, "Count check (no item lost or duplicated): " + (countOk ? "PASS" : "FAIL"));
        log(output, "Exact-order check: " + (orderOk ? "PASS" : "FAIL"));
        log(output, "Elapsed: " + elapsedMillis + "ms");
        log(output, (countOk && orderOk) ? "OVERALL: PASS" : "OVERALL: FAIL");

        if (args.length >= 1) {
            Files.writeString(Path.of(args[0]), output.toString());
        }
    }

    private static void log(StringBuilder output, String line) {
        System.out.println(line);
        output.append(line).append(System.lineSeparator());
    }
}
