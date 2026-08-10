package model;

public class OrderItem {

    private final Product product;
    private final int quantity;
    private final double priceAtPurchase;

    public OrderItem(Product product, int quantity, double priceAtPurchase) {
        this.product = product;
        this.quantity = quantity;
        this.priceAtPurchase = priceAtPurchase;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPriceAtPurchase() {
        return priceAtPurchase;
    }

    public double getSubtotal() {
        return quantity * priceAtPurchase;
    }
}
