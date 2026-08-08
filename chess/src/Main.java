import exceptions.GameOverException;
import exceptions.InvalidMoveException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import model.Color;
import model.Move;
import model.PieceType;
import model.Player;
import observer.ConsoleGameObserver;
import services.ChessGameService;

/**
 * Drives ChessGameService from a plain-text command script so the design can
 * be exercised end-to-end without a UI.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java Main <input-script-path> [output-path]");
            System.exit(1);
        }
        Path inputPath = Path.of(args[0]);
        StringBuilder output = new StringBuilder();

        Player white = new Player("P1", "Alice", Color.WHITE);
        Player black = new Player("P2", "Bob", Color.BLACK);
        ChessGameService service = new ChessGameService(white, black);
        service.addObserver(new ConsoleGameObserver());

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

    private static String execute(ChessGameService service, String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0];
        try {
            return switch (command) {
                case "MOVE" -> {
                    PieceType promotion = parts.length > 3 ? PieceType.valueOf(parts[3]) : null;
                    Move move = service.makeMove(parts[1], parts[2], promotion);
                    yield "OK " + move + " | turn now " + service.getCurrentTurn() + ", status " + service.getStatus();
                }
                case "BOARD" -> service.renderBoard();
                case "STATUS" -> "STATUS turn=" + service.getCurrentTurn() + " status=" + service.getStatus();
                default -> "ERROR unknown command: " + command;
            };
        } catch (InvalidMoveException | GameOverException | IllegalArgumentException e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }
}
