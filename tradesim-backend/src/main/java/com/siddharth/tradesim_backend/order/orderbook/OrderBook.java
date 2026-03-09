package com.siddharth.tradesim_backend.order.orderbook;

import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.exceptions.OrderException;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.*;

@Getter
public class OrderBook {
    private final PriorityQueue<OrderBookEntry> buyOrders;
    private final PriorityQueue<OrderBookEntry> sellOrders;

    private final Map<UUID, OrderBookEntry> buyOrderMap = new HashMap<>();
    private final Map<UUID, OrderBookEntry> sellOrderMap = new HashMap<>();

    public OrderBook() {
        this.buyOrders = new PriorityQueue<>(Comparator.comparing(OrderBookEntry::price).reversed().thenComparing(OrderBookEntry::createdAt));
        this.sellOrders = new PriorityQueue<>(Comparator.comparing(OrderBookEntry::price).thenComparing(OrderBookEntry::createdAt));
    }

    public void addOrder(OrderBookEntry entry) {
        if (entry.side() == OrderSide.BUY) {
            buyOrders.add(entry);
            buyOrderMap.put(entry.orderId(), entry);
        } else {
            sellOrders.add(entry);
            sellOrderMap.put(entry.orderId(), entry);
        }
    }

    public void removeOrder(UUID orderId) {
        OrderBookEntry buyEntry = buyOrderMap.remove(orderId);
        if (buyEntry != null) {
            buyOrders.remove(buyEntry);
            return;
        }

        OrderBookEntry sellEntry = sellOrderMap.remove(orderId);
        if (sellEntry != null) {
            sellOrders.remove(sellEntry);
        }
    }

    public BigDecimal estimateBuyCost(int requiredQuantity) {
        if (requiredQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (sellOrders.isEmpty()) {
            throw new OrderException("No liquidity available for market buy");
        }

        int calculatedRequiredQuantity = requiredQuantity;

        BigDecimal totalCost = BigDecimal.ZERO;

        List<OrderBookEntry> sellOrdersSnapshot = new ArrayList<>(sellOrders);

        sellOrdersSnapshot.sort(Comparator.comparing(OrderBookEntry::price).thenComparing(OrderBookEntry::createdAt));

        for (OrderBookEntry entry : sellOrdersSnapshot) {
            int availableQuantity = entry.quantity();
            int executedQuantity = Math.min(calculatedRequiredQuantity, availableQuantity);
            totalCost = totalCost.add(entry.price().multiply(BigDecimal.valueOf(executedQuantity)));
            calculatedRequiredQuantity -= executedQuantity;
            if (calculatedRequiredQuantity == 0) {
                return totalCost;
            }
        }

        if (calculatedRequiredQuantity == requiredQuantity) {
            throw new OrderException("No liquidity available for market buy");
        }

        return totalCost;
    }
}