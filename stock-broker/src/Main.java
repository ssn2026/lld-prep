import exceptions.AccountNotFoundException;
import exceptions.IllegalOrderStateException;
import exceptions.InsufficientFundsException;
import exceptions.InsufficientHoldingsException;
import exceptions.InvalidOrderException;
import exceptions.NoQuoteAvailableException;
import exceptions.OrderNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import model.Account;
import model.Order;
import model.OrderSide;
import model.OrderType;
import services.StockBrokerService;

/**
 * Drives StockBrokerService from a plain-text command script so the design
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

        StockBrokerService service = new StockBrokerService();

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

    private static String execute(StockBrokerService service, String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0];
        try {
            return switch (command) {
                case "ACCOUNT" -> {
                    String accountId = parts[1];
                    double initialCash = Double.parseDouble(parts[2]);
                    service.openAccount(accountId, initialCash);
                    yield "OK account " + accountId + " opened, cash=" + initialCash;
                }
                case "PRICE" -> {
                    String symbol = parts[1];
                    double price = Double.parseDouble(parts[2]);
                    service.publishPriceUpdate(symbol, price);
                    yield "OK price " + symbol + " -> " + price;
                }
                case "ORDER" -> {
                    String accountId = parts[1];
                    OrderSide side = OrderSide.valueOf(parts[2]);
                    String symbol = parts[3];
                    OrderType type = OrderType.valueOf(parts[4]);
                    int quantity = Integer.parseInt(parts[5]);
                    Double limitPrice = "NONE".equals(parts[6]) ? null : Double.parseDouble(parts[6]);
                    Order order = service.placeOrder(accountId, symbol, side, type, quantity, limitPrice);
                    yield "OK order " + order.getOrderId() + " " + side + " " + quantity + " " + symbol
                            + " (" + type + ") -> status=" + order.getStatus()
                            + (order.getExecutionPrice() != null
                                    ? " @" + order.getExecutionPrice() + " charges=" + order.getCharges()
                                    : "");
                }
                case "CANCEL" -> {
                    service.cancelOrder(parts[1]);
                    yield "OK cancelled order " + parts[1];
                }
                case "PORTFOLIO" -> {
                    Account account = service.getPortfolio(parts[1]);
                    yield "PORTFOLIO " + parts[1] + " cash=" + account.getCashBalance()
                            + " holdings=" + formatHoldings(account.getHoldings());
                }
                case "HISTORY" -> {
                    List<Order> orders = service.getOrderHistory(parts[1]);
                    yield "HISTORY " + parts[1] + "\n" + formatHistory(orders);
                }
                default -> "ERROR unknown command: " + command;
            };
        } catch (AccountNotFoundException | NoQuoteAvailableException | InsufficientFundsException
                | InsufficientHoldingsException | OrderNotFoundException | InvalidOrderException
                | IllegalOrderStateException e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static String formatHoldings(Map<String, Integer> holdings) {
        if (holdings.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        holdings.forEach((symbol, qty) -> sb.append(symbol).append('=').append(qty).append(' '));
        return sb.toString().stripTrailing() + "}";
    }

    private static String formatHistory(List<Order> orders) {
        if (orders.isEmpty()) {
            return "  (no orders)";
        }
        StringBuilder sb = new StringBuilder();
        for (Order order : orders) {
            sb.append("  ").append(order.getOrderId()).append(' ').append(order.getSide())
                    .append(' ').append(order.getQuantity()).append(' ').append(order.getSymbol())
                    .append(" (").append(order.getType()).append(") -> ").append(order.getStatus());
            if (order.getExecutionPrice() != null) {
                sb.append(" @").append(order.getExecutionPrice()).append(" charges=").append(order.getCharges());
            }
            sb.append('\n');
        }
        return sb.toString().stripTrailing();
    }
}
