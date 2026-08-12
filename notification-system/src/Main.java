import builder.NotificationBuilder;
import exceptions.IncompleteNotificationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import model.ChannelType;
import model.Notification;
import model.NotificationPriority;
import services.NotificationService;

/**
 * Drives NotificationService from a plain-text command script so the
 * design can be exercised end-to-end without a UI.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java Main <input-script-path> [output-path]");
            System.exit(1);
        }
        Path inputPath = Path.of(args[0]);
        StringBuilder output = new StringBuilder();

        NotificationService service = new NotificationService();

        List<String> lines = Files.readAllLines(inputPath);
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            log(output, "> " + line);
            String result = execute(service, line);
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

    private static String execute(NotificationService service, String line) {
        String[] parts = line.split("\\s+", 5);
        String command = parts[0];
        try {
            return switch (command) {
                case "SUBSCRIBE" -> {
                    ChannelType type = ChannelType.valueOf(parts[2]);
                    service.subscribe(parts[1], type);
                    yield "OK " + parts[1] + " subscribed to " + type;
                }
                case "UNSUBSCRIBE" -> {
                    ChannelType type = ChannelType.valueOf(parts[2]);
                    boolean removed = service.unsubscribe(parts[1], type);
                    yield removed
                            ? "OK " + parts[1] + " unsubscribed from " + type
                            : "NOOP " + parts[1] + " was not subscribed to " + type;
                }
                case "NOTIFY" -> {
                    String userId = parts[1];
                    NotificationPriority priority = NotificationPriority.valueOf(parts[2]);
                    String title = parts[3];
                    NotificationBuilder builder = new NotificationBuilder().title(title).priority(priority);
                    if (parts.length > 4) {
                        builder.body(parts[4]);
                    }
                    Notification notification = builder.build();
                    int count = service.send(userId, notification);
                    yield "OK notified " + count + " channel(s) for " + userId;
                }
                default -> "ERROR unknown command: " + command;
            };
        } catch (IncompleteNotificationException | IllegalArgumentException e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }
}
