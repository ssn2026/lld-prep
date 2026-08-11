import exceptions.DuplicateJobIdException;
import exceptions.JobNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import model.Job;
import model.JobStatus;
import observer.ConsoleJobListener;
import observer.JobListener;
import services.JobSchedulerService;
import strategy.FixedRateTrigger;
import strategy.OneTimeTrigger;
import strategy.Trigger;

/**
 * Drives JobSchedulerService from a plain-text command script so the design
 * can be exercised end-to-end without a UI. Because job execution happens
 * asynchronously on background threads, the script uses explicit WAIT
 * commands before LOG/STATUS so the transcript stays deterministic.
 */
public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.err.println("Usage: java Main <input-script-path> [output-path]");
            System.exit(1);
        }
        Path inputPath = Path.of(args[0]);
        StringBuilder output = new StringBuilder();

        long startTime = System.currentTimeMillis();
        Queue<String> transcript = new ConcurrentLinkedQueue<>();

        JobSchedulerService service = new JobSchedulerService(2);
        service.addListener(new ConsoleJobListener());
        service.addListener(new TranscriptListener(transcript, startTime));

        List<String> lines = Files.readAllLines(inputPath);
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String result = execute(service, line, transcript, startTime);
            log(output, "> " + line);
            log(output, result);
        }

        if (args.length >= 2) {
            Files.writeString(Path.of(args[1]), output.toString());
        }
    }

    private static void log(StringBuilder output, String line) {
        System.out.println(line);
        output.append(line).append(System.lineSeparator());
    }

    private static String execute(JobSchedulerService service, String line, Queue<String> transcript, long startTime)
            throws InterruptedException {
        String[] parts = line.split("\\s+");
        String command = parts[0];
        try {
            return switch (command) {
                case "SCHEDULE" -> {
                    String jobId = parts[1];
                    String behavior = parts[2];
                    long delay = Long.parseLong(parts[3]);
                    Trigger trigger = parts.length >= 5
                            ? new FixedRateTrigger(delay, Long.parseLong(parts[4]))
                            : new OneTimeTrigger(delay);
                    Job job = "FAIL".equals(behavior)
                            ? () -> { throw new RuntimeException("simulated failure in " + jobId); }
                            : () -> transcript.add(elapsed(startTime) + " EXEC " + jobId);
                    service.scheduleJob(jobId, job, trigger);
                    yield "OK scheduled " + jobId + (trigger.isRecurring() ? " (recurring)" : " (one-time)");
                }
                case "CANCEL" -> {
                    boolean cancelled = service.cancelJob(parts[1]);
                    yield cancelled
                            ? "OK cancelled " + parts[1] + " before it ran"
                            : "NOOP " + parts[1] + " already running/finished; future recurrence (if any) stopped";
                }
                case "WAIT" -> {
                    Thread.sleep(Long.parseLong(parts[1]));
                    yield "OK waited " + parts[1] + "ms";
                }
                case "LOG" -> {
                    StringBuilder sb = new StringBuilder("LOG");
                    String entry;
                    while ((entry = transcript.poll()) != null) {
                        sb.append("\n  ").append(entry);
                    }
                    yield sb.toString();
                }
                case "STATUS" -> {
                    JobStatus status = service.getJobStatus(parts[1]);
                    yield "STATUS " + parts[1] + " -> " + status;
                }
                case "SHUTDOWN" -> {
                    service.shutdown();
                    yield "OK scheduler shut down";
                }
                default -> "ERROR unknown command: " + command;
            };
        } catch (DuplicateJobIdException | JobNotFoundException e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static String elapsed(long startTime) {
        return "[t+" + (System.currentTimeMillis() - startTime) + "ms]";
    }

    private static class TranscriptListener implements JobListener {
        private final Queue<String> transcript;
        private final long startTime;

        TranscriptListener(Queue<String> transcript, long startTime) {
            this.transcript = transcript;
            this.startTime = startTime;
        }

        @Override
        public void onJobCompleted(String jobId) {
            transcript.add(elapsed(startTime) + " COMPLETED " + jobId);
        }

        @Override
        public void onJobFailed(String jobId, Throwable error) {
            transcript.add(elapsed(startTime) + " FAILED " + jobId + " (" + error.getMessage() + ")");
        }
    }
}
