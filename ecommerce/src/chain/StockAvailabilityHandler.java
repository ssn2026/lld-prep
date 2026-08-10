package chain;

import exceptions.OutOfStockException;
import model.CartItem;
import model.Product;

public class StockAvailabilityHandler extends OrderValidationHandler {

    @Override
    protected void validate(OrderValidationContext context) {
        for (CartItem item : context.getCart().getItems()) {
            Product product = context.getProductRepository().findBySku(item.getProduct().getSku());
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new OutOfStockException("Insufficient stock for " + product.getSku()
                        + ": requested " + item.getQuantity() + ", available " + product.getStockQuantity());
            }
        }
    }
}
