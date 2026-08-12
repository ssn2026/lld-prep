import builder.BoardBuilder;
import exceptions.GameAlreadyWonException;
import exceptions.InvalidBoardConfigException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import model.MoveReason;
import observer.ConsoleGameListener;
import observer.GameListener;
import services.SnakeAndLadderService;
import strategy.RandomDiceStrategy;

/**
 * Drives SnakeAndLadderService from a plain-text command script so the
 * design can be exercised end-to-end without a UI.
 */
public class Main {

    private static BoardBuilder boardBuilder = new BoardBuilder(100);
    private static SnakeAndLadderService service;
    private static StringBuilder output;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java Main <input-script-path> [output-path]");
            System.exit(1);
        }
        Path inputPath = Path.of(args[0]);
        output = new StringBuilder();

        List<String> lines = Files.readAllLines(inputPath);
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            log("> " + line);
            String result = execute(line);
            log(result);
        }

        if (args.length >= 2) {
            Files.writeString(Path.of(args[1]), output.toString());
        }
    }

    private static void log(String line) {
        System.out.println(line);
        output.append(line).append(System.lineSeparator());
    }

    private static String execute(String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0];
        try {
            return switch (command) {
                case "BOARD" -> {
                    int a = Integer.parseInt(parts[2]);
                    int b = Integer.parseInt(parts[3]);
                    if ("SNAKE".equals(parts[1])) {
                        boardBuilder.addSnake(a, b);
                        yield "OK snake " + a + " -> " + b;
                    }
                    boardBuilder.addLadder(a, b);
                    yield "OK ladder " + a + " -> " + b;
                }
                case "START" -> {
                    List<String> playerNames = Arrays.asList(parts[1].split(","));
                    long seed = Long.parseLong(parts[2]);
                    service = new SnakeAndLadderService(boardBuilder.build(), playerNames, new RandomDiceStrategy(seed));
                    service.addListener(new ConsoleGameListener());
                    service.addListener(new TranscriptGameListener());
                    yield "OK game started for " + playerNames;
                }
                case "ROLL" -> {
                    String player = service.getCurrentPlayerName();
                    int roll = service.rollAndMove();
                    String won = service.getWinner();
                    yield won != null
                            ? "OK " + player + " rolled " + roll + " -> WINNER: " + won
                            : "OK " + player + " rolled " + roll;
                }
                case "STATUS" -> {
                    Map<String, Integer> positions = service.getPositions();
                    StringBuilder sb = new StringBuilder("STATUS\n");
                    positions.forEach((name, pos) -> sb.append("  ").append(name).append(": ").append(pos).append('\n'));
                    sb.append("  winner: ").append(service.getWinner() == null ? "(none yet)" : service.getWinner());
                    yield sb.toString();
                }
                default -> "ERROR unknown command: " + command;
            };
        } catch (InvalidBoardConfigException | GameAlreadyWonException e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    /** Feeds listener events into the same captured transcript as everything else, not just stdout. */
    private static class TranscriptGameListener implements GameListener {
        @Override
        public void onPositionChanged(String playerName, int from, int to, MoveReason reason) {
            log("  [listener] " + playerName + ": " + from + " -> " + to + " (" + reason + ")");
        }

        @Override
        public void onGameWon(String playerName) {
            log("  [listener] " + playerName + " WINS!");
        }
    }
}
