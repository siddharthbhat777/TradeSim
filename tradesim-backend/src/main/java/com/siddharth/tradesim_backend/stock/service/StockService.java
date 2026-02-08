package com.siddharth.tradesim_backend.stock.service;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.exceptions.CreateStockException;
import com.siddharth.tradesim_backend.stock.exceptions.StockStatusException;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.stock.model.dto.CreateStockRequest;
import com.siddharth.tradesim_backend.stock.model.dto.StockResponse;
import com.siddharth.tradesim_backend.order.repository.TradeRepository;
import com.siddharth.tradesim_backend.order.enums.Status;
import com.siddharth.tradesim_backend.order.model.Trade;
import com.siddharth.tradesim_backend.user.exceptions.StatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockRepository stockRepository;
    private final TradeRepository tradeRepository;

    @Transactional(readOnly = true)
    public List<StockResponse> fetchStocks() {
        return stockRepository.findAll()
                .stream()
                .map(stock -> new StockResponse(
                        stock.getId(),
                        stock.getSymbol(),
                        stock.getCompanyName(),
                        stock.getCurrentPrice(),
                        stock.getSector(),
                        stock.getStatus()
                ))
                .toList();
    }

    @Transactional
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
                    .status(StockStatus.ACTIVE)
                    .build();

            Stock saved = stockRepository.save(stock);

            return new StockResponse(
                    saved.getId(),
                    saved.getSymbol(),
                    saved.getCompanyName(),
                    saved.getCurrentPrice(),
                    saved.getSector(),
                    saved.getStatus()
            );
        } catch (DataIntegrityViolationException e) {
            throw new CreateStockException("Invalid stock data");
        } catch (Exception e) {
            throw new CreateStockException("Unable to add stock");
        }
    }

    @Transactional
    public StockResponse changeStockStatus(UUID stockId, StockStatus status) {
        Stock stock = stockRepository.findById(stockId).orElseThrow(() -> new BusinessException("Stock not found"));
        if (stock.getStatus() == status) throw new StockStatusException("Stock already in this status");
        if (stock.getStatus() == StockStatus.DELISTED)
            throw new StockStatusException("Cannot change stock status of DELISTED stock");
        try {

            if (status == StockStatus.DELISTED) {
                List<Trade> trades = tradeRepository.findByStockIdAndStatus(stockId, Status.PENDING);
                for (Trade trade : trades) {
                    trade.setStatus(Status.CANCELLED);
                }
                tradeRepository.saveAll(trades);
            }
            stock.setStatus(status);
            Stock saved = stockRepository.save(stock);
            return new StockResponse(
                    saved.getId(),
                    saved.getSymbol(),
                    saved.getCompanyName(),
                    saved.getCurrentPrice(),
                    saved.getSector(),
                    saved.getStatus()
            );
        } catch (DataIntegrityViolationException e) {
            throw new StatusException("Invalid status data");
        } catch (Exception e) {
            throw new StatusException("Unable to change status of stock");
        }
    }
}