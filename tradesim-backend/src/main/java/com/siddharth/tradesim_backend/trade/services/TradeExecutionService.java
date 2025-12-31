package com.siddharth.tradesim_backend.trade.services;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.models.User;
import com.siddharth.tradesim_backend.holding.HoldingRepository;
import com.siddharth.tradesim_backend.holding.models.Holding;
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
    private final HoldingRepository holdingRepository;

    @Transactional
    public void executeTrade(
            Trade trade,
            User user,
            BigDecimal executionPrice
    ) {
        if (trade.getStatus() != Status.PENDING) return;

        Stock stock = stockRepository.findById(trade.getStockId()).orElseThrow();

        if (!stock.isActive()) {
            failTrade(trade);
            return;
        }

        if (!isLimitSatisfied(trade, executionPrice)) {
            return;
        }

        boolean success;
        if (trade.getType() == Type.BUY) {
            success = executeBuyTrade(trade, user, stock, executionPrice);
        } else {
            success = executeSellTrade(trade, user, stock, executionPrice);
        }

        if (!success) {
            return;
        }

        finalizeTrade(trade, executionPrice);
        authRepository.save(user);
        tradeRepository.save(trade);
    }

    private boolean executeBuyTrade(
            Trade trade,
            User user,
            Stock stock,
            BigDecimal price
    ) {
        BigDecimal totalCost = price.multiply(BigDecimal.valueOf(trade.getQuantity()));

        if (user.getBalance().compareTo(totalCost) < 0) {
            failTrade(trade);
            return false;
        }

        user.setBalance(user.getBalance().subtract(totalCost));

        Holding holding = holdingRepository.findByUserIdAndStockId(user.getId(), stock.getId())
                .orElse(
                        Holding.builder()
                                .userId(user.getId())
                                .stockId(stock.getId())
                                .quantity(0)
                                .build()
                );

        holding.setQuantity(holding.getQuantity() + trade.getQuantity());
        holdingRepository.save(holding);

        return true;
    }

    private boolean executeSellTrade(
            Trade trade,
            User user,
            Stock stock,
            BigDecimal price
    ) {
        Holding holding = holdingRepository.findByUserIdAndStockId(user.getId(), stock.getId()).orElse(null);

        if (holding == null || holding.getQuantity() < trade.getQuantity()) {
            failTrade(trade);
            return false;
        }

        BigDecimal totalGain = price.multiply(BigDecimal.valueOf(trade.getQuantity()));
        user.setBalance(user.getBalance().add(totalGain));

        holding.setQuantity(holding.getQuantity() - trade.getQuantity());

        if (holding.getQuantity() == 0) {
            holdingRepository.delete(holding);
        } else {
            holdingRepository.save(holding);
        }

        return true;
    }

    private boolean isLimitSatisfied(Trade trade, BigDecimal price) {
        if (trade.getOrderType() != OrderType.LIMIT) return true;
        if (trade.getLimitPrice() == null) return false;

        return (trade.getType() == Type.BUY && price.compareTo(trade.getLimitPrice()) <= 0)
                || (trade.getType() == Type.SELL && price.compareTo(trade.getLimitPrice()) >= 0);
    }

    private void finalizeTrade(Trade trade, BigDecimal price) {
        trade.setPriceAtExecution(price);
        trade.setExecutedAt(Instant.now());
        trade.setStatus(Status.EXECUTED);
    }

    private void failTrade(Trade trade) {
        trade.setStatus(Status.FAILED);
        tradeRepository.save(trade);
    }
}