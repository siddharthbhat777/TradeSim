package com.siddharth.tradesim_backend.trade.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.trade.TradeRepository;
import com.siddharth.tradesim_backend.trade.enums.OrderType;
import com.siddharth.tradesim_backend.trade.enums.Status;
import com.siddharth.tradesim_backend.trade.enums.Type;
import com.siddharth.tradesim_backend.trade.exceptions.TradeException;
import com.siddharth.tradesim_backend.trade.model.Trade;
import com.siddharth.tradesim_backend.trade.model.dto.TradeRequest;
import com.siddharth.tradesim_backend.trade.model.dto.TradeResponse;
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

    public TradeResponse placeOrder(UUID userId, @Valid TradeRequest request) {
        User user = authRepository.findById(userId).orElseThrow(() -> new BusinessException("User not found"));
        Stock stock = stockRepository.findById(request.getStockId()).orElseThrow(() -> new BusinessException("Stock not found"));

        BigDecimal marketPrice = stock.getCurrentPrice();

        Trade trade = Trade.builder()
                .userId(userId)
                .stockId(stock.getId())
                .type(request.getType())
                .orderType(request.getOrderType())
                .quantity(request.getQuantity())
                .status(Status.PENDING)
                .limitPrice(request.getLimitPrice())
                .build();

        tradeRepository.save(trade);
        if (request.getOrderType() == OrderType.MARKET) {
            tradeExecutionService.executeTrade(trade, user, marketPrice);
        } else {
            boolean shouldExecute = (request.getType() == Type.BUY && marketPrice.compareTo(request.getLimitPrice()) <= 0)
                    || (request.getType() == Type.SELL && marketPrice.compareTo(request.getLimitPrice()) >= 0);

            if (shouldExecute) {
                tradeExecutionService.executeTrade(trade, user, marketPrice);
            }
        }

        return mapToResponse(trade, user, stock);
    }

    public TradeResponse cancelTrade(UUID tradeId, UUID userId) {
        User user = authRepository.findById(userId).orElseThrow(() -> new BusinessException("User not found"));
        Trade trade = tradeRepository.findById(tradeId).orElseThrow(() -> new BusinessException("Trade not found"));
        Stock stock = stockRepository.findById(trade.getStockId()).orElseThrow(() -> new BusinessException("Stock not found"));

        if (!trade.getUserId().equals(userId)) {
            throw new TradeException("You are not allowed to cancel this trade");
        }

        if (trade.getStatus() != Status.PENDING) {
            throw new TradeException("Only pending trades can be cancelled");
        }

        trade.setStatus(Status.CANCELLED);
        tradeRepository.save(trade);

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
                user.getBalance(),
                trade.getExecutedAt()
        );
    }
}