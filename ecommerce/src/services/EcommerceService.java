package services;

import builder.OrderBuilder;
import chain.OrderValidationContext;
import chain.OrderValidationHandler;
import chain.PaymentAuthorizationHandler;
import chain.PriceValidationHandler;
import chain.StockAvailabilityHandler;
import exceptions.EmptyCartException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import model.Cart;
import model.CartItem;
import model.Order;
import model.OrderItem;
import model.OrderStatus;
import model.Product;
import repository.OrderRepository;
import repository.ProductRepository;

/**
 * Single public entry point for the ecommerce/inventory system. All
 * caller-facing operations (catalog management, cart, checkout, order
 * lifecycle) go through this class; model/state/chain/builder/repository
 * classes are internal collaborators.
 */
public class EcommerceService {

    private final ProductRepository productRepository = new ProductRepository();
    private final OrderRepository orderRepository = new OrderRepository();
    private final Map<String, Cart> cartsByCustomerId = new LinkedHashMap<>();
    private final AtomicInteger orderSequence = new AtomicInteger(1);
    private final OrderValidationHandler validationChain;

    public EcommerceService() {
        OrderValidationHandler stockCheck = new StockAvailabilityHandler();
        stockCheck.linkWith(new PriceValidationHandler())
                .linkWith(new PaymentAuthorizationHandler());
        this.validationChain = stockCheck;
    }

    public void addProduct(String sku, String name, double price, int initialStock) {
        productRepository.save(new Product(sku, name, price, initialStock));
    }

    public void addToCart(String customerId, String sku, int quantity) {
        Product product = productRepository.findBySku(sku);
        getCart(customerId).addItem(product, quantity);
    }

    public void removeFromCart(String customerId, String sku) {
        getCart(customerId).removeItem(sku);
    }

    public Cart viewCart(String customerId) {
        return getCart(customerId);
    }

    /**
     * Runs the cart through the validation chain (stock -> price/coupon ->
     * payment authorization) before touching any state, so a rejection at
     * any step leaves inventory and the cart untouched.
     */
    public Order checkout(String customerId, String shippingAddress, String couponCode, String paymentMethod) {
        Cart cart = getCart(customerId);
        if (cart.isEmpty()) {
            throw new EmptyCartException("Cart is empty for customer " + customerId);
        }

        OrderValidationContext context =
                new OrderValidationContext(cart, productRepository, couponCode, paymentMethod);
        validationChain.handle(context);

        OrderBuilder builder = new OrderBuilder()
                .orderId("O" + orderSequence.getAndIncrement())
                .customerId(customerId)
                .shippingAddress(shippingAddress)
                .totalAmount(context.getComputedTotal())
                .couponCode(couponCode);

        for (CartItem item : cart.getItems()) {
            Product product = productRepository.findBySku(item.getProduct().getSku());
            product.decreaseStock(item.getQuantity());
            builder.addItem(product, item.getQuantity(), product.getPrice());
        }

        Order order = builder.build();
        orderRepository.save(order);
        cart.clear();
        return order;
    }

    public void confirmOrder(String orderId) {
        orderRepository.findById(orderId).confirm();
    }

    public void shipOrder(String orderId) {
        orderRepository.findById(orderId).ship();
    }

    public void deliverOrder(String orderId) {
        orderRepository.findById(orderId).deliver();
    }

    public void cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId);
        order.cancel();
        for (OrderItem item : order.getItems()) {
            productRepository.findBySku(item.getProduct().getSku()).increaseStock(item.getQuantity());
        }
    }

    public OrderStatus getOrderStatus(String orderId) {
        return orderRepository.findById(orderId).getStatus();
    }

    public String getInventoryReport() {
        StringBuilder sb = new StringBuilder();
        for (Product product : productRepository.getAllProducts()) {
            sb.append(product.getSku()).append(' ').append(product.getName())
                    .append(" $").append(product.getPrice())
                    .append(" stock=").append(product.getStockQuantity())
                    .append('\n');
        }
        return sb.toString();
    }

    private Cart getCart(String customerId) {
        return cartsByCustomerId.computeIfAbsent(customerId, Cart::new);
    }
}
