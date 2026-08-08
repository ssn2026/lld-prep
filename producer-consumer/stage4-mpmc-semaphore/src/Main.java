import buffer.SemaphoreBoundedBuffer;
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
 * Stage 4: same N producers / M consumers correctness bar as Stage 3, this
 * time backed by SemaphoreBoundedBuffer (two Semaphores + a short-held
 * mutex) instead of a single whole-method-synchronized monitor.
 */
public class Main {

    private static final int PRODUCERS = 4;
    private static final int CONSUMERS = 4;
    private static final int ITEMS_PER_PRODUCER = 15;
    private static final int CAPACITY = 5;

    public static void main(String[] args) throws IOException {
        StringBuilder output = new StringBuilder();
        int totalItems = PRODUCERS * ITEMS_PER_PRODUCER;

        SemaphoreBoundedBuffer<String> buffer = new SemaphoreBoundedBuffer<>(CAPACITY);
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

        log(output, "SemaphoreBoundedBuffer capacity: " + CAPACITY + ", " + PRODUCERS + " producers x "
                + CONSUMERS + " consumers, " + totalItems + " total items");

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

        if (args.length >= 1) {
            Files.writeString(Path.of(args[0]), output.toString());
        }
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
