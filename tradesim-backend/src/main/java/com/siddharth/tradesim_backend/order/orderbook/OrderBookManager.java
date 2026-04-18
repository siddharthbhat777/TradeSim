package com.siddharth.tradesim_backend.order.orderbook;

import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class OrderBookManager {
    private static final List<OrderStatus> ACTIVE_ORDER_STATUSES = List.of(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED);
    private static final Object ROLLBACK_REBUILD_KEY = new Object();

    private final OrderRepository orderRepository;
    private final Clock clock;
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
        List<Order> pendingOrders = orderRepository.findByStatusIn(ACTIVE_ORDER_STATUSES)
                .stream()
                .filter(this::isRestingOrder)
                .toList();

        for (Order order : pendingOrders) {
            addOrderToOrderBook(getOrderBook(order.getStockId()), order);
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
        withLock(order.getStockId(), orderBook -> {
            syncOrderState(orderBook, order);
            return null;
        });
    }

    public void removeOrder(Order order) {
        withLock(order.getStockId(), orderBook -> {
            registerRollbackRebuild(order.getStockId());
            removeOrderFromOrderBook(orderBook, order.getId());
            unregisterOrder(order.getId());
            return null;
        });
    }

    public Order getOrderOrLoad(UUID orderId) {
        Order loadedOrder = inMemoryOrders.get(orderId);
        if (loadedOrder != null) {
            return loadedOrder;
        }

        return orderRepository.findById(orderId)
                .filter(this::isRestingOrder)
                .map(order -> {
                    registerOrder(order);
                    return order;
                })
                .orElse(null);
    }

    void syncOrderState(OrderBook orderBook, Order order) {
        registerRollbackRebuild(order.getStockId());
        removeOrderFromOrderBook(orderBook, order.getId());

        if (!isRestingOrder(order)) {
            unregisterOrder(order.getId());
            return;
        }

        addOrderToOrderBook(orderBook, order);
        registerOrder(order);
    }

    private boolean isRestingOrder(Order order) {
        return order.getBookPrice() != null && order.getRemainingQuantity() > 0 && ACTIVE_ORDER_STATUSES.contains(order.getStatus()) && !isExpired(order);
    }

    private boolean isExpired(Order order) {
        return order.getExpiresAt() != null && !order.getExpiresAt().isAfter(clock.instant());
    }

    private void addOrderToOrderBook(OrderBook orderBook, Order order) {
        orderBook.addOrder(toEntry(order));
    }

    private OrderBookEntry toEntry(Order order) {
        return new OrderBookEntry(
                order.getId(),
                order.getUserId(),
                order.getStockId(),
                order.getSide(),
                order.getBookPrice(),
                order.getRemainingQuantity(),
                order.getCreatedAt()
        );
    }

    private void removeOrderFromOrderBook(OrderBook orderBook, UUID orderId) {
        orderBook.removeOrder(orderId);
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

    private void registerRollbackRebuild(UUID stockId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        @SuppressWarnings("unchecked")
        Set<UUID> stockIds = (Set<UUID>) TransactionSynchronizationManager.getResource(ROLLBACK_REBUILD_KEY);
        if (stockIds == null) {
            Set<UUID> trackedStockIds = new LinkedHashSet<>();
            TransactionSynchronizationManager.bindResource(ROLLBACK_REBUILD_KEY, trackedStockIds);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    try {
                        if (status == STATUS_ROLLED_BACK) {
                            trackedStockIds.forEach(OrderBookManager.this::rebuildOrderBook);
                        }
                    } finally {
                        if (TransactionSynchronizationManager.hasResource(ROLLBACK_REBUILD_KEY)) {
                            TransactionSynchronizationManager.unbindResource(ROLLBACK_REBUILD_KEY);
                        }
                    }
                }
            });
            stockIds = trackedStockIds;
        }

        stockIds.add(stockId);
    }

    private void rebuildOrderBook(UUID stockId) {
        ReentrantLock lock = getLock(stockId);
        lock.lock();
        try {
            OrderBook rebuiltOrderBook = new OrderBook();
            Collection<Order> restingOrders = orderRepository.findByStockIdAndStatusIn(stockId, ACTIVE_ORDER_STATUSES)
                    .stream()
                    .filter(this::isRestingOrder)
                    .toList();

            for (Order order : restingOrders) {
                addOrderToOrderBook(rebuiltOrderBook, order);
                registerOrder(order);
            }

            orderBooks.put(stockId, rebuiltOrderBook);
            inMemoryOrders.entrySet().removeIf(entry -> stockId.equals(entry.getValue().getStockId()) && !containsOrder(restingOrders, entry.getKey()));
        } finally {
            lock.unlock();
        }
    }

    private boolean containsOrder(Collection<Order> orders, UUID orderId) {
        return orders.stream().anyMatch(order -> order.getId().equals(orderId));
    }
}