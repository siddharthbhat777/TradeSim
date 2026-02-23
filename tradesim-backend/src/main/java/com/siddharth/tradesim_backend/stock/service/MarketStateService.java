package com.siddharth.tradesim_backend.stock.service;

import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarketStateService {
    private final StockRepository stockRepository;

    @Transactional
    public void recordTrade(UUID stockId, BigDecimal executionPrice, int quantity) {
        Stock stock = stockRepository.findById(stockId).orElseThrow(() -> new IllegalStateException("Stock not found"));
        stock.setCurrentPrice(executionPrice);
        stock.setTotalVolume(stock.getTotalVolume() + quantity);
        stockRepository.save(stock);
    }
}