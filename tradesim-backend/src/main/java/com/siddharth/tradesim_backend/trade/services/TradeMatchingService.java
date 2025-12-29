package com.siddharth.tradesim_backend.trade.services;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.models.User;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.models.Stock;
import com.siddharth.tradesim_backend.trade.TradeRepository;
import com.siddharth.tradesim_backend.trade.enums.OrderType;
import com.siddharth.tradesim_backend.trade.enums.Status;
import com.siddharth.tradesim_backend.trade.models.Trade;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeMatchingService {
    private final TradeExecutionService tradeExecutionService;
    private final AuthRepository authRepository;
    private final TradeRepository tradeRepository;
    private final StockRepository stockRepository;

    @Scheduled(fixedRate = 5000)
    public void processPendingTrades() {
        List<Trade> pendingTrades = tradeRepository.findByStatus(Status.PENDING);
        for (Trade trade : pendingTrades) {
            if (trade.getStatus() != Status.PENDING) continue;
            try {
                Stock stock = stockRepository.findById(trade.getStockId()).orElseThrow();
                User user = authRepository.findById(trade.getUserId()).orElseThrow();

                if (trade.getOrderType() == OrderType.LIMIT &&
                        trade.getLimitPrice() != null &&
                        stock.getCurrentPrice().compareTo(trade.getLimitPrice()) <= 0) {
                    tradeExecutionService.executeTrade(trade, user, stock.getCurrentPrice());
                }
            } catch (Exception e) {
                log.error("Trade {} failed", trade.getId(), e);
            }
        }
    }
}