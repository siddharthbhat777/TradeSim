package com.siddharth.tradesim_backend.stock;

import com.siddharth.tradesim_backend.stock.exceptions.CreateStockException;
import com.siddharth.tradesim_backend.stock.models.Stock;
import com.siddharth.tradesim_backend.stock.models.dto.CreateStockRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockRepository stockRepository;

    public Stock addStock(CreateStockRequest request) {
        if (stockRepository.existsBySymbol(request.symbol())) {
            throw new CreateStockException("Stock with symbol " + request.symbol() + " already exists");
        }

        try {
            Stock stock = Stock.builder()
                    .symbol(request.symbol())
                    .companyName(request.companyName())
                    .currentPrice(request.initialPrice())
                    .sector(request.sector())
                    .active(true)
                    .build();

            return stockRepository.save(stock);
        } catch (DataIntegrityViolationException e) {
            throw new CreateStockException("Invalid stock data");
        } catch (Exception e) {
            throw new CreateStockException("Unable to add stock");
        }
    }

    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }
}