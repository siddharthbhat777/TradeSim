package com.siddharth.tradesim_backend.trade.services;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.models.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.models.Stock;
import com.siddharth.tradesim_backend.trade.TradeRepository;
import com.siddharth.tradesim_backend.trade.enums.OrderType;
import com.siddharth.tradesim_backend.trade.enums.Status;
import com.siddharth.tradesim_backend.trade.enums.Type;
import com.siddharth.tradesim_backend.trade.models.Trade;
import com.siddharth.tradesim_backend.trade.models.dto.BuyTradeRequest;
import com.siddharth.tradesim_backend.trade.models.dto.TradeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TradeService {
    private final AuthRepository authRepository;
    private final StockRepository stockRepository;
    private final TradeRepository tradeRepository;
    private final TradeExecutionService tradeExecutionService;

    public TradeResponse placeBuyOrder(UUID userId, @Valid BuyTradeRequest request) {
        User user = authRepository.findById(userId).orElseThrow(() -> new BusinessException("User not found"));
        Stock stock = stockRepository.findById(request.getStockId()).orElseThrow(() -> new BusinessException("Stock not found"));

        BigDecimal marketPrice = stock.getCurrentPrice();

        Trade trade = Trade.builder()
                .userId(userId)
                .stockId(stock.getId())
                .type(Type.BUY)
                .orderType(request.getOrderType())
                .quantity(request.getQuantity())
                .status(Status.PENDING)
                .build();

        if (request.getOrderType() == OrderType.MARKET) {
            tradeRepository.save(trade);
            tradeExecutionService.executeTrade(trade, user, marketPrice);
        } else {
            trade.setLimitPrice(request.getLimitPrice());
            tradeRepository.save(trade);

            if (marketPrice.compareTo(request.getLimitPrice()) <= 0) {
                tradeExecutionService.executeTrade(trade, user, marketPrice);
            }
        }

        return mapToResponse(trade, user, stock);
    }

    private TradeResponse mapToResponse(Trade trade, User user, Stock stock) {
        BigDecimal totalAmount = null;
        if (trade.getPriceAtExecution() != null) {
            totalAmount = trade.getPriceAtExecution().multiply(BigDecimal.valueOf(trade.getQuantity()));
        }

        return new TradeResponse(
                trade.getId(),
                trade.getStockId(),
                stock.getSymbol(),
                trade.getType(),
                trade.getOrderType(),
                trade.getStatus(),
                trade.getQuantity(),
                trade.getPriceAtExecution(),
                totalAmount,
                user.getBalance()
        );
    }
}