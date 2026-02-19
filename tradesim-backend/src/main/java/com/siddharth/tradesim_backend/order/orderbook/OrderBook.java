package com.siddharth.tradesim_backend.order.orderbook;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.*;

@Getter
public class OrderBook {
    private final PriorityQueue<OrderBookEntry> buyOrders;
    private final PriorityQueue<OrderBookEntry> sellOrders;

    public OrderBook() {
        this.buyOrders = new PriorityQueue<>(Comparator.comparing(OrderBookEntry::price).reversed().thenComparing(OrderBookEntry::createdAt));
        this.sellOrders = new PriorityQueue<>(Comparator.comparing(OrderBookEntry::price).thenComparing(OrderBookEntry::createdAt));
    }

    public void addOrder(OrderBookEntry entry) {
        if (entry.side() == OrderSide.BUY) {
            buyOrders.add(entry);
        } else {
            sellOrders.add(entry);
        }
    }

    public void removeOrder(UUID orderId) {
        buyOrders.removeIf(order -> order.orderId().equals(orderId));
        sellOrders.removeIf(order -> order.orderId().equals(orderId));
    }

    public BigDecimal estimateBuyCost(int requiredQuantity) {
        if (requiredQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (sellOrders.isEmpty()) {
            throw new BusinessException("No liquidity available for market buy");
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
            throw new BusinessException("No liquidity available for market buy");
        }

        return totalCost;
    }
}