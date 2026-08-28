package com.siddharth.tradesim_backend.stock.service;

import com.siddharth.tradesim_backend.market_index.MarketIndexService;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookEntry;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookManager;
import com.siddharth.tradesim_backend.exchange.ExchangeService;
import com.siddharth.tradesim_backend.stock.StockException;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarketStateService {
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final StockRepository stockRepository;
    private final OrderBookManager orderBookManager;
    private final ExchangeService exchangeService;
    private final MarketIndexService marketIndexService;

    @Transactional
    public void recordTrade(UUID stockId, BigDecimal executionPrice, int quantity) {
        Stock stock = stockRepository.findById(stockId).orElseThrow(() -> StockException.notFound("Stock not found"));
        LocalDate today = exchangeService.currentExchangeDate(stock.getExchangeId());

        if (stock.getLastTradingDate() == null || !stock.getLastTradingDate().equals(today)) {
            stock.setDayOpen(executionPrice);
            stock.setDayHigh(executionPrice);
            stock.setDayLow(executionPrice);
            stock.setDayVolume((long) quantity);
            stock.setLastTradingDate(today);
        } else {
            if (stock.getDayHigh() == null || executionPrice.compareTo(stock.getDayHigh()) > 0) {
                stock.setDayHigh(executionPrice);
            }

            if (stock.getDayLow() == null || executionPrice.compareTo(stock.getDayLow()) < 0) {
                stock.setDayLow(executionPrice);
            }

            stock.setDayVolume((stock.getDayVolume() == null ? 0 : stock.getDayVolume()) + quantity);
        }

        stock.setLastTradedPrice(executionPrice);
        stock.setTotalVolume((stock.getTotalVolume() == null ? 0 : stock.getTotalVolume()) + quantity);
        stockRepository.save(stock);

        marketIndexService.updateIndicesForStock(stockId);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateIndicativePrice(UUID stockId) {
        return orderBookManager.withLock(stockId, orderBook -> {
            OrderBookEntry bestBid = orderBook.getBuyOrders().peek();
            OrderBookEntry bestAsk = orderBook.getSellOrders().peek();

            if (bestBid != null && bestAsk != null) {
                return bestBid.price().add(bestAsk.price()).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
            }

            if (bestBid != null) {
                return bestBid.price();
            }

            if (bestAsk != null) {
                return bestAsk.price();
            }

            Stock stock = stockRepository.findById(stockId).orElseThrow(() -> StockException.notFound("Stock not found"));
            return stock.getLastTradedPrice();
        });
    }

    public boolean isWithinPriceBand(UUID stockId, BigDecimal executionPrice) {
        Stock stock = stockRepository.findById(stockId).orElseThrow(() -> StockException.notFound("Stock not found"));

        BigDecimal referencePrice = stock.getLastTradedPrice();
        BigDecimal bandPercent = stock.getPriceBandPercent();

        if (referencePrice == null || bandPercent == null) {
            return true;
        }

        BigDecimal percent = bandPercent.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP);

        BigDecimal maxUp = referencePrice.multiply(BigDecimal.ONE.add(percent));
        BigDecimal maxDown = referencePrice.multiply(BigDecimal.ONE.subtract(percent));

        return executionPrice.compareTo(maxUp) <= 0 && executionPrice.compareTo(maxDown) >= 0;
    }
}