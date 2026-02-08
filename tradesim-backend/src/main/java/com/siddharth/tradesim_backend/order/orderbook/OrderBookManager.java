package com.siddharth.tradesim_backend.order.orderbook;

import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.model.Order;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OrderBookManager {
    private final OrderRepository orderRepository;
    private final Map<UUID, OrderBook> books = new ConcurrentHashMap<>();

    public OrderBook getBook(UUID stockId) {
        return books.computeIfAbsent(stockId, id -> new OrderBook());
    }

    @PostConstruct
    public void loadFromDatabase() {
        List<Order> pendingLimitOrders =
                orderRepository.findByStatusIn(List.of(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED))
                        .stream()
                        .filter(o -> o.getOrderType() == OrderType.LIMIT)
                        .toList();

        for (Order order : pendingLimitOrders) {
            addOrder(order);
        }
    }

    public void addOrder(Order order) {
        if (order.getOrderType() == OrderType.MARKET) return;

        OrderBookEntry entry = new OrderBookEntry(
                order.getId(),
                order.getUserId(),
                order.getStockId(),
                order.getSide(),
                order.getLimitPrice(),
                order.getRemainingQuantity(),
                order.getCreatedAt()
        );

        getBook(order.getStockId()).add(entry);
    }
}