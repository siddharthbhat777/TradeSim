package com.siddharth.tradesim_backend.order.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.holding.HoldingRepository;
import com.siddharth.tradesim_backend.holding.model.Holding;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.exceptions.OrderException;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookManager;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantLock;

import static com.siddharth.tradesim_backend.order.enums.OrderStatus.CANCELLED;
import static com.siddharth.tradesim_backend.order.enums.OrderStatus.FILLED;

@Service
@RequiredArgsConstructor
public class OrderLifecycleService {
    private final OrderRepository orderRepository;
    private final OrderBookManager orderBookManager;
    private final AuthRepository authRepository;
    private final HoldingRepository holdingRepository;

    @Transactional
    public void cancelOrder(Order order) {
        if ((order.getStatus() == FILLED) || (order.getStatus() == CANCELLED)) {
            return;
        }

        ReentrantLock lock = orderBookManager.getLock(order.getStockId());
        lock.lock();
        try {
            releaseLockedAssets(order);
            orderBookManager.removeOrderFromOrderBook(order);
            order.cancel();
            orderBookManager.unregisterOrder(order.getId());
            orderRepository.save(order);
        } finally {
            lock.unlock();
        }
    }

    private void releaseLockedAssets(Order order) {
        int remainingQty = order.getRemainingQuantity();
        if (remainingQty <= 0) return;

        if (order.getSide() == OrderSide.BUY) {
            if (order.getOrderType() == OrderType.MARKET) {
                return;
            }
            if (order.getLimitPrice() == null) {
                throw new OrderException("Limit price missing for LIMIT order");
            }
            User user = authRepository.findById(order.getUserId()).orElseThrow(() -> new BusinessException("User not found"));
            BigDecimal unlockAmount = order.getLimitPrice().multiply(BigDecimal.valueOf(remainingQty));
            user.unlockFunds(unlockAmount);
        } else {
            Holding holding = holdingRepository.findByUserIdAndStockId(order.getUserId(), order.getStockId()).orElseThrow(() -> new BusinessException("Holding not found"));
            holding.unlockShares(remainingQty);
        }
    }
}