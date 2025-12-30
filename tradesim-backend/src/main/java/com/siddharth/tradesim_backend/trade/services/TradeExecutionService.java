package com.siddharth.tradesim_backend.trade.services;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.models.User;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.models.Stock;
import com.siddharth.tradesim_backend.trade.TradeRepository;
import com.siddharth.tradesim_backend.trade.enums.OrderType;
import com.siddharth.tradesim_backend.trade.enums.Status;
import com.siddharth.tradesim_backend.trade.enums.Type;
import com.siddharth.tradesim_backend.trade.models.Trade;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TradeExecutionService {
    private final StockRepository stockRepository;
    private final TradeRepository tradeRepository;
    private final AuthRepository authRepository;

    @Transactional
    public void executeTrade(
            Trade trade,
            User user,
            BigDecimal executionPrice
    ) {
        if (trade.getStatus() != Status.PENDING) {
            return;
        }

        Stock stock = stockRepository.findById(trade.getStockId()).orElseThrow();

        if (!stock.isActive()) {
            trade.setStatus(Status.FAILED);
            tradeRepository.save(trade);
            return;
        }

        if (trade.getOrderType() == OrderType.LIMIT) {
            if (trade.getLimitPrice() == null) {
                trade.setStatus(Status.FAILED);
                tradeRepository.save(trade);
                return;
            }

            boolean limitSatisfied = (trade.getType() == Type.BUY && executionPrice.compareTo(trade.getLimitPrice()) <= 0)
                    || (trade.getType() == Type.SELL && executionPrice.compareTo(trade.getLimitPrice()) >= 0);

            if (!limitSatisfied) {
                return;
            }
        }

        BigDecimal totalCost = executionPrice.multiply(BigDecimal.valueOf(trade.getQuantity()));

        if (trade.getType() == Type.BUY) {
            if (user.getBalance().compareTo(totalCost) < 0) {
                trade.setStatus(Status.FAILED);
                tradeRepository.save(trade);
                return;
            }
            user.setBalance(user.getBalance().subtract(totalCost));
        }

        trade.setPriceAtExecution(executionPrice);
        trade.setExecutedAt(Instant.now());
        trade.setStatus(Status.EXECUTED);

        authRepository.save(user);
        tradeRepository.save(trade);
    }
}