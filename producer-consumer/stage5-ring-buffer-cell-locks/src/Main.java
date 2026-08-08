import buffer.RingBuffer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import worker.RingConsumer;
import worker.RingProducer;

/**
 * Stage 5: the advanced target. capacity (10) is deliberately smaller than
 * the total item count (60) so the ring genuinely wraps around several
 * times during the run - this isn't just a queue with a size limit, cells
 * are actively being reclaimed and reused while consumers are still
 * reading from earlier laps.
 */
public class Main {

    private static final int PRODUCERS = 4;
    private static final int ITEMS_PER_PRODUCER = 15;
    private static final int CONSUMERS = 3;
    private static final int CAPACITY = 10; // smaller than total items -> forces real wraparound

    public static void main(String[] args) throws IOException {
        StringBuilder output = new StringBuilder();
        int totalItems = PRODUCERS * ITEMS_PER_PRODUCER;

        RingBuffer<String> ring = new RingBuffer<>(CAPACITY, CONSUMERS);
        List<String> producedLog = Collections.synchronizedList(new ArrayList<>());
        List<List<String>> consumedLogs = new ArrayList<>();
        for (int c = 0; c < CONSUMERS; c++) {
            consumedLogs.add(new ArrayList<>());
        }

        log(output, "RingBuffer capacity: " + CAPACITY + " (< " + totalItems + " total items, forces wraparound), "
                + PRODUCERS + " producers x " + CONSUMERS + " consumers");

        List<Thread> threads = new ArrayList<>();
        for (int p = 0; p < PRODUCERS; p++) {
            threads.add(new Thread(new RingProducer(ring, p, ITEMS_PER_PRODUCER, producedLog), "producer-" + p));
        }
        for (int c = 0; c < CONSUMERS; c++) {
            threads.add(new Thread(new RingConsumer(ring, c, totalItems, consumedLogs.get(c)), "consumer-" + c));
        }

        long startNanos = System.nanoTime();
        threads.forEach(Thread::start);
        for (Thread t : threads) {
            joinUninterruptibly(t);
        }
        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

        boolean allCorrectSize = true;
        for (int c = 0; c < CONSUMERS; c++) {
            int size = consumedLogs.get(c).size();
            log(output, "Consumer " + c + " read " + size + "/" + totalItems + " items");
            if (size != totalItems) {
                allCorrectSize = false;
            }
        }

        boolean allIdentical = true;
        List<String> reference = consumedLogs.get(0);
        for (int c = 1; c < CONSUMERS; c++) {
            if (!reference.equals(consumedLogs.get(c))) {
                allIdentical = false;
                log(output, "Consumer 0 and consumer " + c + " saw DIFFERENT sequences - broadcast invariant broken");
            }
        }

        Set<String> producedSet = new HashSet<>(producedLog);
        Set<String> consumedSet = new HashSet<>(reference);
        boolean noLossOrFabrication = producedSet.equals(consumedSet) && reference.size() == new HashSet<>(reference).size();

        log(output, "All consumers read " + totalItems + " items: " + (allCorrectSize ? "PASS" : "FAIL"));
        log(output, "All consumers saw an identical, ordered stream: " + (allIdentical ? "PASS" : "FAIL"));
        log(output, "No item lost, duplicated, or fabricated: " + (noLossOrFabrication ? "PASS" : "FAIL"));
        log(output, "Elapsed: " + elapsedMillis + "ms");
        boolean corePass = allCorrectSize && allIdentical && noLossOrFabrication;
        log(output, corePass ? "CORE OVERALL: PASS" : "CORE OVERALL: FAIL");

        log(output, "");
        log(output, "=== resetOffset() / seek demo (on consumer 0, after the run above) ===");
        runResetOffsetDemo(output, ring, reference, totalItems);

        if (args.length >= 1) {
            Files.writeString(Path.of(args[0]), output.toString());
        }
    }

    private static void runResetOffsetDemo(StringBuilder output, RingBuffer<String> ring, List<String> originalConsumerZeroLog, int totalItems) {
        long writeSeq = totalItems; // consumer 0's current offset equals totalItems after the main run
        long floor = writeSeq - CAPACITY; // oldest sequence still physically retained
        long rewindTarget = floor + 2; // a couple past the floor, comfortably inside the retained window

        log(output, "Retained window is roughly [" + floor + ", " + writeSeq + "]; consumer 0 is currently at " + writeSeq);
        log(output, "Rewinding consumer 0 to offset " + rewindTarget + " (a legal seek within the window)");
        ring.resetOffset(0, rewindTarget);

        List<String> reread = new ArrayList<>();
        int itemsToReread = (int) (writeSeq - rewindTarget);
        for (int i = 0; i < itemsToReread; i++) {
            try {
                reread.add(ring.consume(0));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        List<String> expectedTail = originalConsumerZeroLog.subList((int) rewindTarget, originalConsumerZeroLog.size());
        boolean rereadMatches = reread.equals(expectedTail);
        log(output, "Re-read " + reread.size() + " items after rewind; matches original tail exactly: " + (rereadMatches ? "PASS" : "FAIL"));

        long illegalTarget = Math.max(0, floor - 5);
        log(output, "Attempting an out-of-window seek to offset " + illegalTarget + " (older than the retained floor)");
        boolean rejectedCorrectly;
        try {
            ring.resetOffset(0, illegalTarget);
            rejectedCorrectly = false;
            log(output, "  did NOT throw - this is a bug, an out-of-window seek should be rejected");
        } catch (IllegalArgumentException e) {
            rejectedCorrectly = true;
            log(output, "  correctly rejected: " + e.getMessage());
        }

        log(output, (rereadMatches && rejectedCorrectly) ? "RESET-OFFSET DEMO: PASS" : "RESET-OFFSET DEMO: FAIL");
    }

    private static void joinUninterruptibly(Thread t) {
        try {
            t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void log(StringBuilder output, String line) {
        System.out.println(line);
        output.append(line).append(System.lineSeparator());
    }
}
