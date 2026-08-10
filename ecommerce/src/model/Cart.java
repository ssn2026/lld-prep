package model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Cart {

    private final String customerId;
    private final Map<String, CartItem> itemsBySku = new LinkedHashMap<>();

    public Cart(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void addItem(Product product, int quantity) {
        itemsBySku.merge(product.getSku(), new CartItem(product, quantity),
                (existing, added) -> new CartItem(product, existing.getQuantity() + added.getQuantity()));
    }

    public void removeItem(String sku) {
        itemsBySku.remove(sku);
    }

    public List<CartItem> getItems() {
        return new ArrayList<>(itemsBySku.values());
    }

    public boolean isEmpty() {
        return itemsBySku.isEmpty();
    }

    public void clear() {
        itemsBySku.clear();
    }
}
