import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Expense;
import model.SplitType;
import model.User;
import observer.ConsoleNotifier;
import services.SplitwiseService;

/**
 * Drives SplitwiseService from a plain-text command script so the design can
 * be exercised end-to-end without a UI.
 */
public class Main {

    private static final Map<String, User> usersByName = new LinkedHashMap<>();

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java Main <input-script-path> [output-path]");
            System.exit(1);
        }
        Path inputPath = Path.of(args[0]);
        StringBuilder output = new StringBuilder();

        SplitwiseService service = new SplitwiseService();
        service.addObserver(new ConsoleNotifier(line -> log(output, line)));

        List<String> lines = Files.readAllLines(inputPath);
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            log(output, "> " + line);
            log(output, execute(service, line));
        }

        if (args.length >= 2) {
            Files.writeString(Path.of(args[1]), output.toString());
        }
    }

    private static void log(StringBuilder output, String line) {
        System.out.println(line);
        output.append(line).append(System.lineSeparator());
    }

    private static String execute(SplitwiseService service, String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0];
        try {
            return switch (command) {
                case "ADD_USER" -> {
                    User user = service.addUser(parts[1], parts[2]);
                    usersByName.put(parts[1], user);
                    yield "OK user " + parts[1] + " added";
                }
                case "ADD_EXPENSE" -> {
                    String description = parts[1];
                    double amount = Double.parseDouble(parts[2]);
                    String paidByName = parts[3];
                    SplitType splitType = SplitType.valueOf(parts[4]);

                    List<String> participantIds = new ArrayList<>();
                    Map<String, Double> shareInputs = new LinkedHashMap<>();
                    for (String token : parts[5].split(",")) {
                        String name = token;
                        Double value = null;
                        int colon = token.indexOf(':');
                        if (colon >= 0) {
                            name = token.substring(0, colon);
                            value = Double.parseDouble(token.substring(colon + 1));
                        }
                        User participant = resolveUser(name);
                        participantIds.add(participant.getId());
                        if (value != null) {
                            shareInputs.put(participant.getId(), value);
                        }
                    }

                    Expense expense = service.addExpense(description, amount, resolveUser(paidByName).getId(),
                            splitType, participantIds, shareInputs);
                    yield "OK expense " + expense.getId() + " '" + description + "' ($" + amount
                            + ", " + splitType + ") added";
                }
                case "SETTLE" -> {
                    double amount = Double.parseDouble(parts[3]);
                    service.settleUp(resolveUser(parts[1]).getId(), resolveUser(parts[2]).getId(), amount);
                    yield "OK settled";
                }
                case "SHOW_BALANCE" -> service.showBalance(resolveUser(parts[1]).getId(), resolveUser(parts[2]).getId());
                case "SHOW_BALANCES" -> service.showBalancesFor(resolveUser(parts[1]).getId());
                case "SHOW_ALL_BALANCES" -> service.showAllBalances();
                default -> "ERROR unknown command: " + command;
            };
        } catch (RuntimeException e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static User resolveUser(String name) {
        User user = usersByName.get(name);
        if (user == null) {
            throw new IllegalArgumentException("Unknown user: " + name);
        }
        return user;
    }
}
