package com.siddharth.tradesim_backend.order.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.exchange.ExchangeService;
import com.siddharth.tradesim_backend.ledger.LedgerService;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.exceptions.OrderException;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.model.dto.OrderRequest;
import com.siddharth.tradesim_backend.order.model.dto.OrderResponse;
import com.siddharth.tradesim_backend.order.orderbook.MatchResult;
import com.siddharth.tradesim_backend.order.orderbook.OrderBook;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookManager;
import com.siddharth.tradesim_backend.order.orderbook.OrderMatchingEngine;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.risk.service.RiskService;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final AuthRepository authRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;
    private final PositionRepository positionRepository;
    private final OrderBookManager orderBookManager;
    private final OrderMatchingEngine orderMatchingEngine;
    private final RiskService riskService;
    private final ExchangeService exchangeService;
    private final TradingAccountService tradingAccountService;
    private final LedgerService ledgerService;

    @Transactional
    public OrderResponse createOrder(UUID userId, @Valid OrderRequest request) {
        User user = authRepository.findById(userId).orElseThrow(() -> new BusinessException("User not found"));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("User account is not active");
        }

        TradingAccount tradingAccount = tradingAccountService.getTradingAccountByUserId(userId);

        Stock stock = stockRepository.findById(request.stockId()).orElseThrow(() -> new BusinessException("Stock not found"));

        exchangeService.assertTradingAllowed(stock.getExchangeId());

        if (stock.getStatus() != StockStatus.ACTIVE) {
            throw new BusinessException("Stock is not active");
        }

        validateOrder(userId, tradingAccount, stock, request);

        Order order = Order.builder()
                .userId(userId)
                .stockId(stock.getId())
                .side(request.side())
                .orderType(request.orderType())
                .quantity(request.quantity())
                .remainingQuantity(request.quantity())
                .limitPrice(request.limitPrice())
                .status(OrderStatus.OPEN)
                .build();

        orderRepository.save(order);

        if (request.side() == OrderSide.BUY && request.orderType() == OrderType.LIMIT) {
            BigDecimal lockedMargin = calculateRequiredMargin(request.limitPrice(), request.quantity(), tradingAccount.getLeverage());
            tradingAccountService.saveTradingAccount(tradingAccount);
            ledgerService.recordBuyLimitMarginLock(tradingAccount, lockedMargin, stock.getId(), order.getId());
        }

        if (order.getOrderType() == OrderType.LIMIT) {
            orderBookManager.addOrder(order);
        }

        MatchResult result = orderMatchingEngine.match(order);

        riskService.checkLiquidation(userId);

        return new OrderResponse(
                order.getId(),
                order.getStockId(),
                order.getSide(),
                order.getOrderType(),
                order.getStatus(),
                order.getQuantity(),
                order.getRemainingQuantity(),
                order.getLimitPrice(),
                result.priceBandHit() ? "Price band limit reached. Remaining quantity pending." : null
        );
    }

    @Transactional
    public void cancelOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("You are not allowed to cancel this order");
        }

        if (order.getStatus() != OrderStatus.OPEN && order.getStatus() != OrderStatus.PARTIALLY_FILLED) {
            throw new OrderException("Only open or partially filled orders can be cancelled");
        }

        if (order.getOrderType() == OrderType.LIMIT) {
            switch (order.getSide()) {
                case BUY -> {
                    TradingAccount tradingAccount = tradingAccountService.getTradingAccountByUserId(userId);
                    BigDecimal remainingReserved = calculateRequiredMargin(order.getLimitPrice(), order.getRemainingQuantity(), tradingAccount.getLeverage());
                    tradingAccount.unlockFunds(remainingReserved);
                    tradingAccountService.saveTradingAccount(tradingAccount);
                    ledgerService.recordBuyLimitMarginUnlock(tradingAccount, remainingReserved, order.getStockId(), order.getId());
                }
                case SELL -> {
                    Position position = positionRepository.findByUserIdAndStockId(userId, order.getStockId()).orElseThrow(() -> new BusinessException("Position not found"));
                    position.unlockShares(order.getRemainingQuantity());
                    positionRepository.save(position);
                }
            }
        }

        ReentrantLock lock = orderBookManager.getLock(order.getStockId());
        lock.lock();
        try {
            orderBookManager.removeOrder(order);
            order.cancel();
            orderRepository.save(order);
        } finally {
            lock.unlock();
        }
    }

    private void validateOrder(UUID userId, TradingAccount tradingAccount, Stock stock, @Valid OrderRequest request) {
        switch (request.orderType()) {
            case MARKET -> validateMarketOrder(userId, tradingAccount, stock, request);
            case LIMIT -> validateLimitOrder(userId, tradingAccount, stock, request);
        }
    }

    private void validateMarketOrder(UUID userId, TradingAccount tradingAccount, Stock stock, @Valid OrderRequest request) {
        switch (request.side()) {
            case BUY -> validateMarketBuy(tradingAccount, stock, request);
            case SELL -> validateUserPosition(userId, stock.getId(), request.quantity());
        }
    }

    private void validateMarketBuy(TradingAccount tradingAccount, Stock stock, @Valid OrderRequest request) {
        BigDecimal estimatedCost = estimateMarketBuyCost(stock.getId(), request.quantity());
        riskService.validateBuyOrder(tradingAccount, estimatedCost);
    }

    private void validateLimitOrder(UUID userId, TradingAccount tradingAccount, Stock stock, @Valid OrderRequest request) {
        if (request.limitPrice() == null) {
            throw new OrderException("Limit price missing for LIMIT order");
        }
        switch (request.side()) {
            case BUY -> validateLimitBuy(tradingAccount, request);
            case SELL -> validateLimitSell(userId, stock.getId(), request.quantity());
        }
    }

    private void validateLimitBuy(TradingAccount tradingAccount, @Valid OrderRequest request) {
        BigDecimal requiredMargin = calculateRequiredMargin(request.limitPrice(), request.quantity(), tradingAccount.getLeverage());
        tradingAccount.lockFunds(requiredMargin);
    }

    private void validateLimitSell(UUID userId, UUID stockId, int quantity) {
        Position position = positionRepository.findByUserIdAndStockId(userId, stockId).orElseThrow(() -> new BusinessException("No shares to sell"));
        position.lockShares(quantity);
        positionRepository.save(position);
    }

    private BigDecimal estimateMarketBuyCost(UUID stockId, int requiredQuantity) {
        ReentrantLock lock = orderBookManager.getLock(stockId);
        lock.lock();
        try {
            OrderBook orderBook = orderBookManager.getOrderBook(stockId);
            return orderBook.estimateBuyCost(requiredQuantity);
        } finally {
            lock.unlock();
        }
    }

    private void validateUserPosition(UUID userId, UUID stockId, int quantity) {
        Position position = positionRepository.findByUserIdAndStockId(userId, stockId).orElseThrow(() -> new BusinessException("No shares to sell"));
        if (position.getAvailableQuantity() < quantity) {
            throw new BusinessException("Insufficient shares to sell");
        }
    }

    private BigDecimal calculateRequiredMargin(BigDecimal price, int quantity, int leverage) {
        return price.multiply(BigDecimal.valueOf(quantity)).divide(BigDecimal.valueOf(leverage), 4, RoundingMode.HALF_UP);
    }
}