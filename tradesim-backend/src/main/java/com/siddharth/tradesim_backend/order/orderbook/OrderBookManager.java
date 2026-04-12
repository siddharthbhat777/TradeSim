package com.siddharth.tradesim_backend.order.orderbook;

import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class OrderBookManager {
    private final OrderRepository orderRepository;
    private final Map<UUID, OrderBook> orderBooks = new ConcurrentHashMap<>();
    private final Map<UUID, ReentrantLock> stockLocks = new ConcurrentHashMap<>();
    private final Map<UUID, Order> inMemoryOrders = new ConcurrentHashMap<>();

    public <T> T withLock(UUID stockId, java.util.function.Function<OrderBook, T> action) {
        ReentrantLock lock = getLock(stockId);
        lock.lock();
        try {
            return action.apply(getOrderBook(stockId));
        } finally {
            lock.unlock();
        }
    }

    public OrderBook getOrderBook(UUID stockId) {
        return orderBooks.computeIfAbsent(stockId, _ -> new OrderBook());
    }

    @PostConstruct
    public void loadPendingOrdersFromDatabase() {
        List<Order> pendingOrders = orderRepository.findByStatusIn(List.of(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED))
                .stream()
                .filter(order -> order.getBookPrice() != null)
                .toList();

        for (Order order : pendingOrders) {
            addOrderToOrderBook(order);
            registerOrder(order);
        }
    }

    public Order getOrder(UUID orderId) {
        return inMemoryOrders.get(orderId);
    }

    public ReentrantLock getLock(UUID stockId) {
        return stockLocks.computeIfAbsent(stockId, _ -> new ReentrantLock());
    }

    public void addOrder(Order order) {
        addOrderToOrderBook(order);
        registerOrder(order);
    }

    public void removeOrder(Order order) {
        removeOrderFromOrderBook(order);
        unregisterOrder(order.getId());
    }

    public Order getOrderOrLoad(UUID orderId) {
        Order loadedOrder = inMemoryOrders.get(orderId);
        if (loadedOrder != null) {
            return loadedOrder;
        }

        return orderRepository.findById(orderId)
                .filter(order -> order.getBookPrice() != null)
                .filter(order -> order.getStatus() == OrderStatus.OPEN || order.getStatus() == OrderStatus.PARTIALLY_FILLED)
                .map(order -> {
                    registerOrder(order);
                    return order;
                })
                .orElse(null);
    }

    private void addOrderToOrderBook(Order order) {
        if (order.getBookPrice() == null) {
            return;
        }

        OrderBookEntry entry = new OrderBookEntry(
                order.getId(),
                order.getUserId(),
                order.getStockId(),
                order.getSide(),
                order.getBookPrice(),
                order.getRemainingQuantity(),
                order.getCreatedAt()
        );

        OrderBook orderBook = getOrderBook(order.getStockId());
        orderBook.addOrder(entry);
    }

    private void removeOrderFromOrderBook(Order order) {
        OrderBook orderBook = orderBooks.get(order.getStockId());
        if (orderBook != null) {
            orderBook.removeOrder(order.getId());
        }
    }

    private void registerOrder(Order order) {
        inMemoryOrders.put(order.getId(), order);
    }

    private void unregisterOrder(UUID orderId) {
        if (orderId == null) {
            return;
        }
        inMemoryOrders.remove(orderId);
    }
}