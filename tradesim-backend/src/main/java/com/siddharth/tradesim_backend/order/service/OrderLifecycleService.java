package com.siddharth.tradesim_backend.order.service;

import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.exchange.model.Exchange;
import com.siddharth.tradesim_backend.forex.service.ForexService;
import com.siddharth.tradesim_backend.forex.service.FxFeeService;
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
import com.siddharth.tradesim_backend.stock.StockException;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import com.siddharth.tradesim_backend.wallet.model.Wallet;
import com.siddharth.tradesim_backend.wallet.model.WalletBucket;
import com.siddharth.tradesim_backend.wallet.WalletService;
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
    private final WalletService walletService;
    private final PositionRepository positionRepository;
    private final LedgerService ledgerService;
    private final StockRepository stockRepository;
    private final ExchangeRepository exchangeRepository;
    private final ForexService forexService;
    private final FxFeeService fxFeeService;

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

        Stock stock = stockRepository.findById(order.getStockId()).orElseThrow(() -> StockException.notFound("Stock not found"));
        Exchange exchange = exchangeRepository.findById(stock.getExchangeId()).orElseThrow(() -> com.siddharth.tradesim_backend.exchange.ExchangeException.notFound("Exchange not found"));

        TradingAccount tradingAccount = tradingAccountService.getTradingAccountByUserIdForUpdate(order.getUserId());
        Wallet wallet = walletService.getWalletByUserId(order.getUserId());
        WalletBucket bucket = walletService.getBucketForUpdate(wallet.getId(), tradingAccount.getBaseCurrency());

        BigDecimal blockValueInStockCurrency = order.getReservationPrice().multiply(BigDecimal.valueOf(remainingQty));
        BigDecimal unlockAmountInStockCurrency = blockValueInStockCurrency.divide(BigDecimal.valueOf(tradingAccount.getLeverage()), 4, RoundingMode.HALF_UP);

        BigDecimal unlockMarginInAccountCurrency = forexService.convert(
                unlockAmountInStockCurrency,
                exchange.getCurrency(),
                tradingAccount.getBaseCurrency()
        );

        BigDecimal fxFee = fxFeeService.calculateConversionFee(
                tradingAccount.getBaseCurrency(),
                exchange.getCurrency(),
                unlockMarginInAccountCurrency
        );

        BigDecimal totalUnlockAmount = unlockMarginInAccountCurrency.add(fxFee);

        bucket.setLockedBalance(bucket.getLockedBalance().subtract(totalUnlockAmount));

        if (order.getOrderType() == OrderType.LIMIT) {
            ledgerService.recordBuyLimitMarginUnlock(bucket, tradingAccount, totalUnlockAmount, order.getStockId(), order.getId());
            return;
        }

        ledgerService.recordBuyOrderMarginUnlock(bucket, tradingAccount, totalUnlockAmount, order.getStockId(), order.getId());
    }

    private void releaseSellReservation(Order order, int remainingQty) {
        boolean sharesWereReserved = order.getOrderType() == OrderType.LIMIT || order.getTimeInForce() == TimeInForce.DAY || order.getTimeInForce() == TimeInForce.GTC;
        if (!sharesWereReserved) {
            return;
        }

        Position position = positionRepository.findByUserIdAndStockId(order.getUserId(), order.getStockId()).orElseThrow(() -> PositionException.notFound("Position not found"));

        position.unlockShares(remainingQty);
        positionRepository.save(position);
    }
}