package com.siddharth.tradesim_backend.trade.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.trade.TradeRepository;
import com.siddharth.tradesim_backend.trade.enums.OrderType;
import com.siddharth.tradesim_backend.trade.enums.Status;
import com.siddharth.tradesim_backend.trade.enums.Type;
import com.siddharth.tradesim_backend.trade.model.Trade;
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

                if (!stock.isActive()) {
                    tradeExecutionService.executeTrade(
                            trade,
                            user,
                            stock.getCurrentPrice()
                    );
                    continue;
                }

                if (trade.getOrderType() == OrderType.MARKET) {
                    continue;
                }

                BigDecimal currentPrice = stock.getCurrentPrice();
                BigDecimal limitPrice = trade.getLimitPrice();

                if (limitPrice == null) continue;

                boolean shouldExecute = (trade.getType() == Type.BUY && currentPrice.compareTo(limitPrice) <= 0)
                        || (trade.getType() == Type.SELL && currentPrice.compareTo(limitPrice) >= 0);

                if (shouldExecute) {
                    tradeExecutionService.executeTrade(trade, user, stock.getCurrentPrice());
                }
            } catch (Exception e) {
                log.error("Trade {} failed", trade.getId(), e);
            }
        }
    }
}