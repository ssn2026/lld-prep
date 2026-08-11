import exceptions.IllegalMatchOperationException;
import exceptions.MatchNotInProgressException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import observer.ConsoleMatchListener;
import services.CricInfoService;

/**
 * Drives CricInfoService from a plain-text command script so the design
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

        CricInfoService service = new CricInfoService();
        service.addListener(new ConsoleMatchListener());

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

    private static String execute(CricInfoService service, String line) {
        String[] parts = line.split("\\s+", 3);
        String command = parts[0];
        try {
            return switch (command) {
                case "TEAM" -> {
                    String side = parts[1];
                    String[] tokens = parts[2].split("\\s+", 2);
                    String teamName = tokens[0];
                    List<String> players = Arrays.asList(tokens[1].split(","));
                    if ("A".equals(side)) {
                        teamAName = teamName;
                        teamAPlayers = players;
                    } else {
                        service.setTeams(teamAName, teamAPlayers, teamName, players);
                    }
                    yield "OK team " + side + " = " + teamName + " (" + players.size() + " players)";
                }
                case "MATCH" -> {
                    int oversLimit = Integer.parseInt(parts[1]);
                    service.startMatch(oversLimit);
                    yield "OK match started, " + oversLimit + " overs a side";
                }
                case "BALL" -> {
                    String type = parts[1];
                    int runs = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                    service.recordBall(type, runs);
                    yield "OK ball recorded: " + type + (parts.length > 2 ? " " + runs : "");
                }
                case "NEXTINNINGS" -> {
                    service.startSecondInnings();
                    yield "OK second innings started";
                }
                case "SCORECARD" -> "SCORECARD\n" + indent(service.getScorecard());
                default -> "ERROR unknown command: " + command;
            };
        } catch (IllegalMatchOperationException | MatchNotInProgressException e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static String indent(String text) {
        return "  " + text.replace("\n", "\n  ");
    }

    private static String teamAName;
    private static List<String> teamAPlayers;
}
