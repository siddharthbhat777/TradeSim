package com.siddharth.tradesim_backend.stock.service;

import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.model.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockPriceFluctuationService {
    private final StockRepository stockRepository;

    private static final BigDecimal MAX_PERCENT = new BigDecimal("0.01");
    private static final BigDecimal MIN_PERCENT = new BigDecimal("0.001");

    @Transactional
    @Scheduled(fixedRate = 5000)
    public void fluctuatePrices() {
        List<Stock> stocks = stockRepository.findByStatus(StockStatus.ACTIVE);

        for (Stock stock : stocks) {
            BigDecimal oldPrice = stock.getCurrentPrice();
            BigDecimal newPrice = calculateNewPrice(oldPrice);

            stock.setCurrentPrice(newPrice);
            stockRepository.save(stock);

            log.info("Stock {} price changed {} → {}", stock.getSymbol(), oldPrice, newPrice);
        }
    }

    private BigDecimal calculateNewPrice(BigDecimal price) {
        BigDecimal percent = MIN_PERCENT.add(MAX_PERCENT.subtract(MIN_PERCENT).multiply(BigDecimal.valueOf(Math.random())));

        boolean increase = Math.random() > 0.5;
        BigDecimal delta = price.multiply(percent);

        BigDecimal newPrice = increase ? price.add(delta) : price.subtract(delta);

        return newPrice.max(BigDecimal.valueOf(1));
    }
}