package com.siddharth.tradesim_backend.order.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.holding.HoldingRepository;
import com.siddharth.tradesim_backend.holding.model.Holding;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.model.dto.OrderRequest;
import com.siddharth.tradesim_backend.order.model.dto.OrderResponse;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.orderbook.OrderBook;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookManager;
import com.siddharth.tradesim_backend.order.orderbook.OrderMatchingEngine;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.model.Stock;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final AuthRepository authRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;
    private final HoldingRepository holdingRepository;
    private final OrderBookManager orderBookManager;
    private final OrderMatchingEngine orderMatchingEngine;

    @Transactional
    public OrderResponse createOrder(UUID userId, @Valid OrderRequest request) {
        User user = authRepository.findById(userId).orElseThrow(() -> new BusinessException("User not found"));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("User account is not active");
        }

        Stock stock = stockRepository.findById(request.getStockId()).orElseThrow(() -> new BusinessException("Stock not found"));

        if (stock.getStatus() != StockStatus.ACTIVE) {
            throw new BusinessException("Stock is not active");
        }

        validateOrder(user, stock, request);

        Order order = Order.builder()
                .userId(userId)
                .stockId(stock.getId())
                .side(request.getSide())
                .orderType(request.getOrderType())
                .quantity(request.getQuantity())
                .remainingQuantity(request.getQuantity())
                .limitPrice(request.getLimitPrice())
                .status(OrderStatus.OPEN)
                .build();

        orderRepository.save(order);

        if (order.getOrderType() == OrderType.LIMIT) {
            orderBookManager.addOrderToOrderBook(order);
            orderBookManager.registerOrder(order);
        }

        orderMatchingEngine.match(order);

        return new OrderResponse(
                order.getId(),
                order.getStockId(),
                order.getSide(),
                order.getOrderType(),
                order.getStatus(),
                order.getQuantity(),
                order.getRemainingQuantity(),
                order.getLimitPrice()
        );
    }

    @Transactional
    public void cancelOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new BusinessException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("You are not allowed to cancel this order");
        }

        if (order.getStatus() != OrderStatus.OPEN && order.getStatus() != OrderStatus.PARTIALLY_FILLED) {
            throw new BusinessException("Only open or partially filled orders can be cancelled");
        }

        if (order.getOrderType() == OrderType.LIMIT) {
            switch (order.getSide()) {
                case BUY -> {
                    User user = authRepository.findById(userId).orElseThrow(() -> new BusinessException("User not found"));
                    BigDecimal remainingReserved = order.getLimitPrice().multiply(BigDecimal.valueOf(order.getRemainingQuantity()));
                    user.unlockFunds(remainingReserved);
                    authRepository.save(user);
                }
                case SELL -> {
                    Holding holding = holdingRepository.findByUserIdAndStockId(userId, order.getStockId()).orElseThrow(() -> new BusinessException("Holding not found"));
                    holding.unlockShares(order.getRemainingQuantity());
                    holdingRepository.save(holding);
                }
            }
        }

        orderBookManager.removeOrderFromOrderBook(order);
        orderBookManager.unregisterOrder(order.getId());
        order.cancel();
        orderRepository.save(order);
    }

    private void validateOrder(User user, Stock stock, @Valid OrderRequest request) {
        switch (request.getOrderType()) {
            case MARKET -> validateMarketOrder(user, stock, request);
            case LIMIT -> validateLimitOrder(user, stock, request);
        }
    }

    private void validateMarketOrder(User user, Stock stock, @Valid OrderRequest request) {
        switch (request.getSide()) {
            case BUY -> validateMarketBuy(user, stock, request);
            case SELL -> validateUserHolding(user.getId(), stock.getId(), request.getQuantity());
        }
    }

    private void validateMarketBuy(User user, Stock stock, @Valid OrderRequest request) {
        BigDecimal estimatedCost = estimateMarketBuyCost(stock.getId(), request.getQuantity());
        if (user.getAvailableBalance().compareTo(estimatedCost) < 0) {
            throw new BusinessException("Insufficient balance for market order");
        }
    }

    private void validateLimitOrder(User user, Stock stock, @Valid OrderRequest request) {
        switch (request.getSide()) {
            case BUY -> validateLimitBuy(user, request);
            case SELL -> validateLimitSell(user.getId(), stock.getId(), request.getQuantity());
        }
    }

    private void validateLimitBuy(User user, @Valid OrderRequest request) {
        BigDecimal orderValue = request.getLimitPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        user.lockFunds(orderValue);
    }

    private void validateLimitSell(UUID userId, UUID stockId, int quantity) {
        Holding holding = holdingRepository.findByUserIdAndStockId(userId, stockId).orElseThrow(() -> new BusinessException("No shares to sell"));
        holding.lockShares(quantity);
        holdingRepository.save(holding);
    }

    private BigDecimal estimateMarketBuyCost(UUID stockId, int requiredQuantity) {
        OrderBook orderBook = orderBookManager.getOrderBook(stockId);
        return orderBook.estimateBuyCost(requiredQuantity);
    }

    private void validateUserHolding(UUID userId, UUID stockId, int quantity) {
        Holding holding = holdingRepository.findByUserIdAndStockId(userId, stockId).orElseThrow(() -> new BusinessException("No shares to sell"));
        if (holding.getAvailableQuantity() < quantity) {
            throw new BusinessException("Insufficient shares to sell");
        }
    }
}
