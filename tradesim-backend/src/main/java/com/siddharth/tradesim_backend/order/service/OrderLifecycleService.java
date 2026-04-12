package com.siddharth.tradesim_backend.order.service;

import com.siddharth.tradesim_backend.ledger.LedgerService;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.enums.TimeInForce;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookManager;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.PositionException;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.locks.ReentrantLock;

import static com.siddharth.tradesim_backend.order.enums.OrderStatus.CANCELLED;
import static com.siddharth.tradesim_backend.order.enums.OrderStatus.FILLED;

@Service
@RequiredArgsConstructor
public class OrderLifecycleService {
    private final OrderRepository orderRepository;
    private final OrderBookManager orderBookManager;
    private final TradingAccountService tradingAccountService;
    private final PositionRepository positionRepository;
    private final LedgerService ledgerService;

    @Transactional
    public void cancelOrder(Order order) {
        if ((order.getStatus() == FILLED) || (order.getStatus() == CANCELLED)) {
            return;
        }

        ReentrantLock lock = orderBookManager.getLock(order.getStockId());
        lock.lock();
        try {
            releaseLockedAssets(order);
            orderBookManager.removeOrder(order);
            order.cancel();
            orderRepository.save(order);
        } finally {
            lock.unlock();
        }
    }

    private void releaseLockedAssets(Order order) {
        int remainingQty = order.getRemainingQuantity();
        if (remainingQty <= 0) {
            return;
        }

        if (order.getSide() == OrderSide.BUY) {
            releaseBuyReservation(order, remainingQty);
            return;
        }

        releaseSellReservation(order, remainingQty);
    }

    private void releaseBuyReservation(Order order, int remainingQty) {
        if (order.getReservationPrice() == null) {
            return;
        }

        TradingAccount tradingAccount = tradingAccountService.getTradingAccountByUserId(order.getUserId());
        BigDecimal unlockAmount = order.getReservationPrice()
                .multiply(BigDecimal.valueOf(remainingQty))
                .divide(BigDecimal.valueOf(tradingAccount.getLeverage()), 4, RoundingMode.HALF_UP);

        tradingAccount.unlockFunds(unlockAmount);
        tradingAccountService.saveTradingAccount(tradingAccount);

        if (order.getOrderType() == OrderType.LIMIT) {
            ledgerService.recordBuyLimitMarginUnlock(tradingAccount, unlockAmount, order.getStockId(), order.getId());
            return;
        }

        ledgerService.recordBuyOrderMarginUnlock(tradingAccount, unlockAmount, order.getStockId(), order.getId());
    }

    private void releaseSellReservation(Order order, int remainingQty) {
        boolean sharesWereReserved = order.getOrderType() == OrderType.LIMIT || order.getTimeInForce() == TimeInForce.DAY;
        if (!sharesWereReserved) {
            return;
        }

        Position position = positionRepository.findByUserIdAndStockId(order.getUserId(), order.getStockId()).orElseThrow(() -> PositionException.notFound("Position not found"));

        position.unlockShares(remainingQty);
        positionRepository.save(position);
    }
}