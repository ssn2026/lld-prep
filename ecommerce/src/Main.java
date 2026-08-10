import exceptions.EmptyCartException;
import exceptions.InvalidOrderStateException;
import exceptions.OrderNotFoundException;
import exceptions.OutOfStockException;
import exceptions.PaymentDeclinedException;
import exceptions.ProductNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import model.Cart;
import model.CartItem;
import model.Order;
import services.EcommerceService;

/**
 * Drives EcommerceService from a plain-text command script so the design can
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

        EcommerceService service = new EcommerceService();

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

    private static String execute(EcommerceService service, String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0];
        try {
            return switch (command) {
                case "PRODUCT" -> {
                    String sku = parts[1];
                    String name = parts[2];
                    double price = Double.parseDouble(parts[3]);
                    int stock = Integer.parseInt(parts[4]);
                    service.addProduct(sku, name, price, stock);
                    yield "OK product " + sku + " (" + name + ") added, price=$" + price + " stock=" + stock;
                }
                case "ADDCART" -> {
                    String customerId = parts[1];
                    String sku = parts[2];
                    int qty = Integer.parseInt(parts[3]);
                    service.addToCart(customerId, sku, qty);
                    yield "OK added " + qty + "x " + sku + " to " + customerId + "'s cart";
                }
                case "REMOVECART" -> {
                    String customerId = parts[1];
                    String sku = parts[2];
                    service.removeFromCart(customerId, sku);
                    yield "OK removed " + sku + " from " + customerId + "'s cart";
                }
                case "CART" -> {
                    Cart cart = service.viewCart(parts[1]);
                    yield "CART " + parts[1] + "\n" + formatCart(cart);
                }
                case "CHECKOUT" -> {
                    String customerId = parts[1];
                    String shippingAddress = parts[2];
                    String couponCode = "NONE".equals(parts[3]) ? null : parts[3];
                    String paymentMethod = parts[4];
                    Order order = service.checkout(customerId, shippingAddress, couponCode, paymentMethod);
                    yield "OK checkout -> order " + order.getOrderId() + " total=$" + order.getTotalAmount()
                            + " status=" + order.getStatus();
                }
                case "CONFIRM" -> {
                    service.confirmOrder(parts[1]);
                    yield "OK order " + parts[1] + " confirmed";
                }
                case "SHIP" -> {
                    service.shipOrder(parts[1]);
                    yield "OK order " + parts[1] + " shipped";
                }
                case "DELIVER" -> {
                    service.deliverOrder(parts[1]);
                    yield "OK order " + parts[1] + " delivered";
                }
                case "CANCEL" -> {
                    service.cancelOrder(parts[1]);
                    yield "OK order " + parts[1] + " cancelled, stock restocked";
                }
                case "STATUS" -> "STATUS " + parts[1] + " -> " + service.getOrderStatus(parts[1]);
                case "INVENTORY" -> "INVENTORY\n" + service.getInventoryReport().stripTrailing();
                default -> "ERROR unknown command: " + command;
            };
        } catch (ProductNotFoundException | OutOfStockException | InvalidOrderStateException
                | PaymentDeclinedException | OrderNotFoundException | EmptyCartException e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static String formatCart(Cart cart) {
        if (cart.isEmpty()) {
            return "  (empty)";
        }
        StringBuilder sb = new StringBuilder();
        for (CartItem item : cart.getItems()) {
            sb.append("  ").append(item.getQuantity()).append("x ")
                    .append(item.getProduct().getSku()).append(" (").append(item.getProduct().getName())
                    .append(") @ $").append(item.getProduct().getPrice()).append('\n');
        }
        return sb.toString().stripTrailing();
    }
}
