import exceptions.EmptyDeckException;
import exceptions.IllegalHandActionException;
import exceptions.PlayerNotFoundException;
import exceptions.RoundNotReadyException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import services.BlackjackService;

/**
 * Drives BlackjackService from a plain-text command script so the design
 * can be exercised end-to-end without a UI.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java Main <input-script-path> [output-path]");
            System.exit(1);
        }
        Path inputPath = Path.of(args[0]);
        StringBuilder output = new StringBuilder();

        BlackjackService service = new BlackjackService();

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

    private static String execute(BlackjackService service, String line) {
        String[] parts = line.split("\\s+", 2);
        String command = parts[0];
        try {
            return switch (command) {
                case "ROUND" -> {
                    List<String> playerNames = Arrays.asList(parts[1].split(","));
                    service.startRound(playerNames);
                    yield "OK round started for " + playerNames;
                }
                case "HIT" -> {
                    service.hit(parts[1]);
                    yield "OK " + parts[1] + " hits";
                }
                case "STAND" -> {
                    service.stand(parts[1]);
                    yield "OK " + parts[1] + " stands";
                }
                case "DEALER" -> {
                    service.playDealerTurn();
                    yield "OK dealer played";
                }
                case "HANDS" -> "HANDS\n" + indent(service.getHandsSummary());
                case "RESULT" -> "RESULT\n" + indent(service.getRoundResult());
                default -> "ERROR unknown command: " + command;
            };
        } catch (PlayerNotFoundException | IllegalHandActionException | RoundNotReadyException | EmptyDeckException e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static String indent(String text) {
        return "  " + text.replace("\n", "\n  ");
    }
}
