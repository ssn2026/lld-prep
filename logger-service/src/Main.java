import exceptions.InvalidLogLevelException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import model.LogLevel;
import observer.ConsoleLogAppender;
import observer.FileLogAppender;
import services.LoggerService;
import strategy.JsonFormatter;
import strategy.PlainTextFormatter;

/**
 * Drives LoggerService from a plain-text command script so the design can
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

        LoggerService logger = LoggerService.getInstance();

        List<String> lines = Files.readAllLines(inputPath);
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            log(output, "> " + line);
            String result = execute(logger, line);
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

    private static String execute(LoggerService logger, String line) {
        String[] parts = line.split("\\s+", 3);
        String command = parts[0];
        try {
            return switch (command) {
                case "LEVEL" -> {
                    LogLevel level = parseLevel(parts[1]);
                    logger.setMinLevel(level);
                    yield "OK min level = " + level;
                }
                case "FORMAT" -> {
                    if ("JSON".equals(parts[1])) {
                        logger.setFormatter(new JsonFormatter());
                    } else {
                        logger.setFormatter(new PlainTextFormatter());
                    }
                    yield "OK formatter = " + parts[1];
                }
                case "APPENDER" -> {
                    if ("CONSOLE".equals(parts[1])) {
                        logger.addAppender(new ConsoleLogAppender());
                        yield "OK console appender added";
                    }
                    Path filePath = Path.of(parts[2]);
                    logger.addAppender(new FileLogAppender(filePath));
                    yield "OK file appender added -> " + filePath;
                }
                case "LOG" -> {
                    LogLevel level = parseLevel(parts[1]);
                    String message = parts.length > 2 ? parts[2] : "";
                    logger.log(level, message);
                    yield "OK logged";
                }
                case "DUMPFILE" -> {
                    Path filePath = Path.of(parts[1]);
                    String content = Files.readString(filePath);
                    yield "DUMPFILE " + filePath + "\n" + indent(content.stripTrailing());
                }
                default -> "ERROR unknown command: " + command;
            };
        } catch (InvalidLogLevelException e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        } catch (IOException e) {
            return "ERROR IOException: " + e.getMessage();
        }
    }

    private static LogLevel parseLevel(String text) {
        try {
            return LogLevel.valueOf(text);
        } catch (IllegalArgumentException e) {
            throw new InvalidLogLevelException(text);
        }
    }

    private static String indent(String text) {
        return "  " + text.replace("\n", "\n  ");
    }
}
