package repository;

import exceptions.OrderNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import model.Order;

public class OrderRepository {

    private final Map<String, Order> ordersById = new LinkedHashMap<>();

    public void save(Order order) {
        ordersById.put(order.getOrderId(), order);
    }

    public Order findById(String orderId) {
        Order order = ordersById.get(orderId);
        if (order == null) {
            throw new OrderNotFoundException("No order with id " + orderId);
        }
        return order;
    }
}
