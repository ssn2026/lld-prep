package repository;

import exceptions.OrderNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Order;
import model.OrderStatus;

public class OrderRepository {

    private final Map<String, Order> ordersById = new LinkedHashMap<>();

    public void save(Order order) {
        ordersById.put(order.getOrderId(), order);
    }

    public Order findByOrderId(String orderId) {
        Order order = ordersById.get(orderId);
        if (order == null) {
            throw new OrderNotFoundException("No order with id " + orderId);
        }
        return order;
    }

    public List<Order> findPendingBySymbol(String symbol) {
        List<Order> result = new ArrayList<>();
        for (Order order : ordersById.values()) {
            if (order.getStatus() == OrderStatus.PENDING && order.getSymbol().equals(symbol)) {
                result.add(order);
            }
        }
        return result;
    }

    public List<Order> findByAccountId(String accountId) {
        List<Order> result = new ArrayList<>();
        for (Order order : ordersById.values()) {
            if (order.getAccountId().equals(accountId)) {
                result.add(order);
            }
        }
        return result;
    }
}
