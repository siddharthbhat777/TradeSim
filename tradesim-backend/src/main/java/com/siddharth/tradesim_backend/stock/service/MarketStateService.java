package com.siddharth.tradesim_backend.stock.service;

import com.siddharth.tradesim_backend.order.orderbook.OrderBook;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookEntry;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookManager;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarketStateService {
    private final StockRepository stockRepository;
    private final OrderBookManager orderBookManager;

    @Transactional
    public void recordTrade(UUID stockId, BigDecimal executionPrice, int quantity) {
        Stock stock = stockRepository.findById(stockId).orElseThrow(() -> new IllegalStateException("Stock not found"));
        stock.setLastTradedPrice(executionPrice);
        stock.setTotalVolume(stock.getTotalVolume() + quantity);
        stockRepository.save(stock);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateIndicativePrice(UUID stockId) {
        OrderBook orderBook = orderBookManager.getOrderBook(stockId);

        OrderBookEntry bestBid = orderBook.getBuyOrders().peek();
        OrderBookEntry bestAsk = orderBook.getSellOrders().peek();

        if (bestBid != null && bestAsk != null) {
            return bestBid.price()
                    .add(bestAsk.price())
                    .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        }

        if (bestBid != null) {
            return bestBid.price();
        }

        if (bestAsk != null) {
            return bestAsk.price();
        }

        Stock stock = stockRepository.findById(stockId).orElseThrow(() -> new IllegalStateException("Stock not found"));

        return stock.getLastTradedPrice();
    }
}