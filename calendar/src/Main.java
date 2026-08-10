import exceptions.CalendarGroupNotFoundException;
import exceptions.EventConflictException;
import exceptions.EventNotFoundException;
import exceptions.InvalidEventException;
import exceptions.UserNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import model.Event;
import model.RecurrenceType;
import services.CalendarService;

/**
 * Drives CalendarService from a plain-text command script so the design can
 * be exercised end-to-end without a UI. Event titles use underscores instead
 * of spaces (e.g. Team_Standup) to keep whitespace-delimited parsing simple.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java Main <input-script-path> [output-path]");
            System.exit(1);
        }
        Path inputPath = Path.of(args[0]);
        StringBuilder output = new StringBuilder();

        CalendarService service = new CalendarService();

        List<String> lines = Files.readAllLines(inputPath);
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String result = execute(service, line);
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

    private static String execute(CalendarService service, String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0];
        try {
            return switch (command) {
                case "USER" -> {
                    service.registerUser(parts[1], parts[2]);
                    yield "OK user " + parts[1] + " (" + parts[2] + ") registered";
                }
                case "EVENT" -> {
                    String ownerId = parts[1];
                    String title = parts[2].replace('_', ' ');
                    LocalDateTime start = LocalDateTime.parse(parts[3]);
                    LocalDateTime end = LocalDateTime.parse(parts[4]);
                    Set<String> attendees = "NONE".equals(parts[5]) ? Set.of() : Set.of(parts[5].split(","));
                    RecurrenceType recurrenceType = RecurrenceType.valueOf(parts[6]);
                    int occurrenceCount = Integer.parseInt(parts[7]);
                    List<Event> occurrences = service.createEvent(ownerId, title, null, start, end,
                            attendees, recurrenceType, occurrenceCount);
                    StringBuilder ids = new StringBuilder();
                    occurrences.forEach(e -> ids.append(e.getEventId()).append(' '));
                    yield "OK created " + occurrences.size() + " occurrence(s): " + ids.toString().stripTrailing();
                }
                case "CANCEL" -> {
                    service.cancelEvent(parts[1]);
                    yield "OK cancelled event " + parts[1];
                }
                case "CANCELSERIES" -> {
                    service.cancelSeries(parts[1]);
                    yield "OK cancelled series " + parts[1];
                }
                case "VIEW" -> {
                    String viewerId = parts[1];
                    String ownerId = parts[2];
                    LocalDateTime rangeStart = LocalDateTime.parse(parts[3]);
                    LocalDateTime rangeEnd = LocalDateTime.parse(parts[4]);
                    List<Event> events = service.viewCalendar(viewerId, ownerId, rangeStart, rangeEnd);
                    yield "VIEW " + ownerId + " (as seen by " + viewerId + ")\n" + formatEvents(events);
                }
                case "GROUP" -> {
                    service.createGroup(parts[1]);
                    yield "OK group " + parts[1] + " created";
                }
                case "GROUPADD" -> {
                    service.addGroupMember(parts[1], parts[2]);
                    yield "OK added " + parts[2] + " to group " + parts[1];
                }
                case "GROUPVIEW" -> {
                    LocalDateTime rangeStart = LocalDateTime.parse(parts[2]);
                    LocalDateTime rangeEnd = LocalDateTime.parse(parts[3]);
                    List<Event> events = service.getGroupEvents(parts[1], rangeStart, rangeEnd);
                    yield "GROUPVIEW " + parts[1] + "\n" + formatEvents(events);
                }
                default -> "ERROR unknown command: " + command;
            };
        } catch (UserNotFoundException | EventConflictException | EventNotFoundException
                | InvalidEventException | CalendarGroupNotFoundException e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static String formatEvents(List<Event> events) {
        if (events.isEmpty()) {
            return "  (no events)";
        }
        StringBuilder sb = new StringBuilder();
        for (Event event : events) {
            sb.append("  ").append(event.getEventId()).append(' ').append(event.getTitle())
                    .append(" [").append(event.getStart()).append(" - ").append(event.getEnd()).append(']');
            if (event.getSeriesId() != null) {
                sb.append(" series=").append(event.getSeriesId());
            }
            sb.append('\n');
        }
        return sb.toString().stripTrailing();
    }
}
