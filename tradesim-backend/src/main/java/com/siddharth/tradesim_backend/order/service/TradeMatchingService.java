package com.siddharth.tradesim_backend.order.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.order.TradeRepository;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.enums.Status;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.model.Trade;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeMatchingService {
    private final TradeExecutionService tradeExecutionService;
    private final AuthRepository authRepository;
    private final TradeRepository tradeRepository;
    private final StockRepository stockRepository;

    @Transactional
    @Scheduled(fixedRate = 5000)
    public void processPendingTrades() {
        List<Trade> pendingTrades = tradeRepository.findByStatus(Status.PENDING);
        for (Trade trade : pendingTrades) {
            if (trade.getStatus() != Status.PENDING) continue;
            try {
                Stock stock = stockRepository.findById(trade.getStockId()).orElseThrow();
                User user = authRepository.findById(trade.getUserId()).orElseThrow();
                if (user.getAccountStatus() != AccountStatus.ACTIVE) continue;

                if (stock.getStatus() != StockStatus.ACTIVE) {
                    continue;
                }

                if (trade.getOrderType() == OrderType.MARKET) {
                    continue;
                }

                BigDecimal currentPrice = stock.getCurrentPrice();
                BigDecimal limitPrice = trade.getLimitPrice();

                if (limitPrice == null) continue;

                boolean shouldExecute = (trade.getType() == OrderSide.BUY && currentPrice.compareTo(limitPrice) <= 0)
                        || (trade.getType() == OrderSide.SELL && currentPrice.compareTo(limitPrice) >= 0);

                if (shouldExecute) {
                    tradeExecutionService.executeTrade(trade, user, stock.getCurrentPrice());
                }
            } catch (Exception e) {
                log.error("Trade {} failed", trade.getId(), e);
            }
        }
    }
}