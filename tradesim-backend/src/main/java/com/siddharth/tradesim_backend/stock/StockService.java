package com.siddharth.tradesim_backend.stock;

import com.siddharth.tradesim_backend.stock.exceptions.CreateStockException;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.stock.model.dto.CreateStockRequest;
import com.siddharth.tradesim_backend.stock.model.dto.StockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockRepository stockRepository;

    public List<StockResponse> fetchStocks() {
        return stockRepository.findAll()
                .stream()
                .map(stock -> new StockResponse(
                        stock.getId(),
                        stock.getSymbol(),
                        stock.getCompanyName(),
                        stock.getCurrentPrice(),
                        stock.getSector(),
                        stock.isActive()
                ))
                .toList();
    }

    public StockResponse addStock(CreateStockRequest request) {
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

            Stock saved = stockRepository.save(stock);

            return new StockResponse(
                    saved.getId(),
                    saved.getSymbol(),
                    saved.getCompanyName(),
                    saved.getCurrentPrice(),
                    saved.getSector(),
                    saved.isActive()
            );
        } catch (DataIntegrityViolationException e) {
            throw new CreateStockException("Invalid stock data");
        } catch (Exception e) {
            throw new CreateStockException("Unable to add stock");
        }
    }
}