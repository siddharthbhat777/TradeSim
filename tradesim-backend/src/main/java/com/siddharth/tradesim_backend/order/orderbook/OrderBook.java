package com.siddharth.tradesim_backend.order.orderbook;

import com.siddharth.tradesim_backend.order.enums.OrderSide;
import lombok.Getter;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.UUID;

@Getter
public class OrderBook {
    private final PriorityQueue<OrderBookEntry> buyOrders;
    private final PriorityQueue<OrderBookEntry> sellOrders;

    public OrderBook() {
        this.buyOrders = new PriorityQueue<>(Comparator.comparing(OrderBookEntry::price).reversed().thenComparing(OrderBookEntry::createdAt));
        this.sellOrders = new PriorityQueue<>(Comparator.comparing(OrderBookEntry::price).thenComparing(OrderBookEntry::createdAt));
    }

    public void add(OrderBookEntry entry) {
        if (entry.side() == OrderSide.BUY) {
            buyOrders.add(entry);
        } else {
            sellOrders.add(entry);
        }
    }

    public void remove(UUID orderId) {
        buyOrders.removeIf(o -> o.orderId().equals(orderId));
        sellOrders.removeIf(o -> o.orderId().equals(orderId));
    }
}