import buffer.SafeBoundedBuffer;
import buffer.UnsafeBoundedBuffer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import worker.TaggedConsumer;
import worker.TaggedProducer;

/**
 * Stage 3: generalizes Stage 1's single-lock bounded buffer to N producers
 * and M consumers (work-queue semantics - each item consumed exactly once,
 * unlike Stage 5's broadcast/offset model). Runs two experiments:
 *   1. SafeBoundedBuffer (notifyAll) under real N:M load - correctness only.
 *   2. UnsafeBoundedBuffer (notify) stress-tested for several short trials
 *      with a hang timeout, to honestly show the lost-wakeup risk instead
 *      of just asserting it in a comment.
 */
public class Main {

    private static final int PRODUCERS = 4;
    private static final int CONSUMERS = 4;
    private static final int ITEMS_PER_PRODUCER = 15;
    private static final int CAPACITY = 5;

    public static void main(String[] args) throws IOException {
        StringBuilder output = new StringBuilder();

        log(output, "=== Experiment 1: SafeBoundedBuffer (notifyAll), " + PRODUCERS + " producers x "
                + CONSUMERS + " consumers ===");
        runSafeExperiment(output);

        log(output, "");
        log(output, "=== Experiment 2: UnsafeBoundedBuffer (notify) stress test ===");
        runUnsafeStressTest(output);

        if (args.length >= 1) {
            Files.writeString(Path.of(args[0]), output.toString());
        }
    }

    private static void runSafeExperiment(StringBuilder output) {
        int totalItems = PRODUCERS * ITEMS_PER_PRODUCER;
        SafeBoundedBuffer<String> buffer = new SafeBoundedBuffer<>(CAPACITY);
        List<String> producedLog = Collections.synchronizedList(new ArrayList<>());
        List<String> consumedLog = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger ticketDispenser = new AtomicInteger(0);

        List<Thread> threads = new ArrayList<>();
        for (int p = 0; p < PRODUCERS; p++) {
            threads.add(new Thread(new TaggedProducer(buffer, p, ITEMS_PER_PRODUCER, producedLog), "producer-" + p));
        }
        for (int c = 0; c < CONSUMERS; c++) {
            threads.add(new Thread(new TaggedConsumer(buffer, ticketDispenser, totalItems, consumedLog), "consumer-" + c));
        }

        long startNanos = System.nanoTime();
        threads.forEach(Thread::start);
        for (Thread t : threads) {
            joinUninterruptibly(t);
        }
        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

        Set<String> producedSet = new HashSet<>(producedLog);
        Set<String> consumedSet = new HashSet<>(consumedLog);
        boolean noDuplicates = consumedSet.size() == consumedLog.size();
        boolean countMatches = consumedLog.size() == totalItems && producedLog.size() == totalItems;
        boolean setsMatch = producedSet.equals(consumedSet);

        log(output, "Produced: " + producedLog.size() + "/" + totalItems + ", Consumed: " + consumedLog.size() + "/" + totalItems);
        log(output, "No duplicate consumption: " + (noDuplicates ? "PASS" : "FAIL"));
        log(output, "Count matches (no loss): " + (countMatches ? "PASS" : "FAIL"));
        log(output, "Consumed set == produced set: " + (setsMatch ? "PASS" : "FAIL"));
        log(output, "Elapsed: " + elapsedMillis + "ms");
        log(output, (noDuplicates && countMatches && setsMatch) ? "OVERALL: PASS" : "OVERALL: FAIL");
    }

    private static void runUnsafeStressTest(StringBuilder output) {
        int trials = 6;
        int producers = 3;
        int consumers = 3;
        int itemsPerProducer = 10;
        int totalItems = producers * itemsPerProducer;
        long perThreadTimeoutMs = 1500;
        int hangCount = 0;

        for (int trial = 1; trial <= trials; trial++) {
            UnsafeBoundedBuffer<String> buffer = new UnsafeBoundedBuffer<>(1); // capacity 1 maximizes contention
            List<String> producedLog = Collections.synchronizedList(new ArrayList<>());
            List<String> consumedLog = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger ticketDispenser = new AtomicInteger(0);

            List<Thread> threads = new ArrayList<>();
            for (int p = 0; p < producers; p++) {
                Thread t = new Thread(new TaggedProducer(buffer, p, itemsPerProducer, producedLog), "u-producer-" + p);
                t.setDaemon(true); // so a hung trial can't prevent the JVM from exiting
                threads.add(t);
            }
            for (int c = 0; c < consumers; c++) {
                Thread t = new Thread(new TaggedConsumer(buffer, ticketDispenser, totalItems, consumedLog), "u-consumer-" + c);
                t.setDaemon(true);
                threads.add(t);
            }

            threads.forEach(Thread::start);
            boolean allFinished = true;
            for (Thread t : threads) {
                try {
                    t.join(perThreadTimeoutMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (t.isAlive()) {
                    allFinished = false;
                }
            }

            if (allFinished) {
                log(output, "  trial " + trial + ": completed (produced=" + producedLog.size()
                        + ", consumed=" + consumedLog.size() + ")");
            } else {
                hangCount++;
                log(output, "  trial " + trial + ": DID NOT COMPLETE within " + perThreadTimeoutMs
                        + "ms (produced=" + producedLog.size() + "/" + totalItems
                        + ", consumed=" + consumedLog.size() + "/" + totalItems + ") - likely lost wakeup");
            }
        }
        log(output, "Unsafe buffer hung on " + hangCount + "/" + trials + " trial(s).");
        log(output, "This is inherently non-deterministic - see GUIDE.md for why a 0-hang result here");
        log(output, "does not mean notify() was actually safe, just that this run got lucky.");
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
