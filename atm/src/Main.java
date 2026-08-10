import exceptions.AccountNotFoundException;
import exceptions.IllegalAtmOperationException;
import exceptions.InsufficientCashException;
import exceptions.InsufficientFundsException;
import exceptions.InvalidAmountException;
import exceptions.InvalidPinException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import model.TransactionReceipt;
import services.AtmService;

/**
 * Drives AtmService from a plain-text command script so the design can be
 * exercised end-to-end without a UI.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java Main <input-script-path> [output-path]");
            System.exit(1);
        }
        Path inputPath = Path.of(args[0]);
        StringBuilder output = new StringBuilder();

        AtmService service = new AtmService();

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

    private static String execute(AtmService service, String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0];
        try {
            return switch (command) {
                case "ACCOUNT" -> {
                    String accountNumber = parts[1];
                    String pin = parts[2];
                    int initialBalance = Integer.parseInt(parts[3]);
                    service.registerAccount(accountNumber, pin, initialBalance);
                    yield "OK account " + accountNumber + " registered, balance=" + initialBalance;
                }
                case "CASH" -> {
                    int denomination = Integer.parseInt(parts[1]);
                    int count = Integer.parseInt(parts[2]);
                    service.loadCash(denomination, count);
                    yield "OK loaded " + count + "x " + denomination + " notes";
                }
                case "INSERT" -> {
                    service.insertCard(parts[1]);
                    yield "OK card inserted for " + parts[1];
                }
                case "PIN" -> {
                    service.enterPin(parts[1]);
                    yield "OK PIN accepted, session authenticated";
                }
                case "BALANCE" -> "BALANCE " + service.checkBalance();
                case "WITHDRAW" -> {
                    int amount = Integer.parseInt(parts[1]);
                    TransactionReceipt receipt = service.withdraw(amount);
                    yield "OK withdrew " + amount + " -> balance=" + receipt.getBalanceAfter()
                            + " dispensed=" + formatBreakdown(receipt.getDenominationBreakdown());
                }
                case "DEPOSIT" -> {
                    int amount = Integer.parseInt(parts[1]);
                    TransactionReceipt receipt = service.deposit(amount);
                    yield "OK deposited " + amount + " -> balance=" + receipt.getBalanceAfter();
                }
                case "STATEMENT" -> {
                    List<TransactionReceipt> history = service.getMiniStatement();
                    yield "STATEMENT\n" + formatStatement(history);
                }
                case "EJECT" -> {
                    service.ejectCard();
                    yield "OK card ejected";
                }
                case "RESET" -> {
                    service.resetMachine();
                    yield "OK machine reset to idle";
                }
                case "STATUS" -> "STATUS -> " + service.getStatus();
                default -> "ERROR unknown command: " + command;
            };
        } catch (AccountNotFoundException | InvalidPinException | InsufficientFundsException
                | InsufficientCashException | IllegalAtmOperationException | InvalidAmountException e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static String formatBreakdown(Map<Integer, Integer> breakdown) {
        if (breakdown == null || breakdown.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        breakdown.forEach((denomination, count) -> sb.append(count).append('x').append(denomination).append(' '));
        return sb.toString().stripTrailing() + "}";
    }

    private static String formatStatement(List<TransactionReceipt> history) {
        if (history.isEmpty()) {
            return "  (no transactions)";
        }
        StringBuilder sb = new StringBuilder();
        for (TransactionReceipt receipt : history) {
            sb.append("  ").append(receipt.getType()).append(' ').append(receipt.getAmount())
                    .append(" -> balance=").append(receipt.getBalanceAfter());
            if (receipt.getDenominationBreakdown() != null) {
                sb.append(' ').append(formatBreakdown(receipt.getDenominationBreakdown()));
            }
            sb.append('\n');
        }
        return sb.toString().stripTrailing();
    }
}
