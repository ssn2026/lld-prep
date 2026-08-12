import exceptions.GameOverException;
import exceptions.InvalidMoveException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import model.GameStatus;
import model.Mark;
import observer.ConsoleGameListener;
import observer.GameListener;
import services.TicTacToeService;
import strategy.LineWinningStrategy;

/**
 * Drives TicTacToeService from a plain-text command script so the design
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

        TicTacToeService service = new TicTacToeService(3, new LineWinningStrategy());
        service.addListener(new ConsoleGameListener());
        service.addListener(new TranscriptGameListener(output));

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

    private static String execute(TicTacToeService service, String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0];
        try {
            return switch (command) {
                case "MOVE" -> {
                    int row = Integer.parseInt(parts[1]);
                    int col = Integer.parseInt(parts[2]);
                    service.makeMove(row, col);
                    yield "OK move (" + row + "," + col + ") -> " + service.getStatus();
                }
                case "STATUS" -> "STATUS " + service.getStatus() + "\n" + indent(service.renderBoard());
                default -> "ERROR unknown command: " + command;
            };
        } catch (InvalidMoveException | GameOverException e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static String indent(String text) {
        return "  " + text.replace("\n", "\n  ");
    }

    /** Feeds listener events into the same captured transcript as everything else, not just stdout. */
    private static class TranscriptGameListener implements GameListener {
        private final StringBuilder output;

        TranscriptGameListener(StringBuilder output) {
            this.output = output;
        }

        @Override
        public void onMove(Mark mark, int row, int col) {
            log(output, "  [listener] " + mark + " played (" + row + "," + col + ")");
        }

        @Override
        public void onGameOver(GameStatus result) {
            log(output, "  [listener] game over: " + result);
        }
    }
}
