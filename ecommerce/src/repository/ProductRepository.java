package repository;

import exceptions.ProductNotFoundException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import model.Product;

public class ProductRepository {

    private final Map<String, Product> productsBySku = new LinkedHashMap<>();

    public void save(Product product) {
        productsBySku.put(product.getSku(), product);
    }

    public Product findBySku(String sku) {
        Product product = productsBySku.get(sku);
        if (product == null) {
            throw new ProductNotFoundException("No product with SKU " + sku);
        }
        return product;
    }

    public Collection<Product> getAllProducts() {
        return productsBySku.values();
    }
}
