package com.siddharth.tradesim_backend.order;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.model.dto.OrderRequest;
import com.siddharth.tradesim_backend.order.model.dto.OrderResponse;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.model.Stock;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final AuthRepository authRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;

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

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new BusinessException("Only open orders can be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setRemainingQuantity(0);

        orderRepository.save(order);
    }
}
