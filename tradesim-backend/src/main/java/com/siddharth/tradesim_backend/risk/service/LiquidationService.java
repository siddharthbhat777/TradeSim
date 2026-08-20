package com.siddharth.tradesim_backend.risk.service;

import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.exchange.model.Exchange;
import com.siddharth.tradesim_backend.forex.service.ForexService;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.enums.TimeInForce;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookEntry;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookManager;
import com.siddharth.tradesim_backend.order.orderbook.OrderMatchingEngine;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.order.service.OrderLifecycleService;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.service.MarketStateService;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import com.siddharth.tradesim_backend.wallet.model.Wallet;
import com.siddharth.tradesim_backend.wallet.model.WalletBucket;
import com.siddharth.tradesim_backend.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class LiquidationService {
    private final PositionRepository positionRepository;
    private final StockRepository stockRepository;
    private final TradingAccountService tradingAccountService;
    private final WalletService walletService;
    private final OrderMatchingEngine orderMatchingEngine;
    private final OrderBookManager orderBookManager;
    private final OrderRepository orderRepository;
    private final OrderLifecycleService orderLifecycleService;
    private final MarketStateService marketStateService;
    private final ExchangeRepository exchangeRepository;
    private final ForexService forexService;
    private final Set<UUID> liquidatingUsers = ConcurrentHashMap.newKeySet();

    public void liquidateUser(UUID userId) {
        if (!liquidatingUsers.add(userId)) {
            return;
        }

        try {
            List<Position> positions = positionRepository.findByUserId(userId);

            if (positions.isEmpty()) return;

            TradingAccount tradingAccount = tradingAccountService.getTradingAccountByUserId(userId);
            String userCurrency = tradingAccount.getBaseCurrency();

            positions.sort((position1, position2) -> {
                BigDecimal loss1 = getUnrealizedLoss(position1, userCurrency);
                BigDecimal loss2 = getUnrealizedLoss(position2, userCurrency);
                return loss2.compareTo(loss1);
            });

            for (Position position : positions) {

                if (position.getQuantity() <= 0) continue;

                while (true) {
                    Position freshPosition = positionRepository.findById(position.getId()).orElse(null);
                    if (freshPosition == null || freshPosition.getQuantity() <= 0) {
                        break;
                    }

                    if (!shouldLiquidate(tradingAccount)) {
                        return;
                    }

                    boolean hasExecutableLiquidity = orderBookManager.withLock(
                            freshPosition.getStockId(),
                            orderBook -> {
                                OrderBookEntry bestBid = orderBook.getBuyOrders().peek();
                                if (bestBid == null) {
                                    return false;
                                }
                                if (bestBid.userId() != null && bestBid.userId().equals(userId)) {
                                    return false;
                                }
                                return marketStateService.isWithinPriceBand(freshPosition.getStockId(), bestBid.price());
                            }
                    );
                    if (!hasExecutableLiquidity) {
                        break;
                    }

                    int sellQuantity = Math.max(1, freshPosition.getQuantity() / 10);

                    Order liquidationOrder = Order.builder()
                            .userId(userId)
                            .stockId(freshPosition.getStockId())
                            .side(OrderSide.SELL)
                            .orderType(OrderType.MARKET)
                            .timeInForce(TimeInForce.IOC)
                            .quantity(sellQuantity)
                            .remainingQuantity(sellQuantity)
                            .status(OrderStatus.OPEN)
                            .build();

                    orderRepository.save(liquidationOrder);
                    orderMatchingEngine.match(liquidationOrder);

                    int executedQuantity = liquidationOrder.getQuantity() - liquidationOrder.getRemainingQuantity();
                    if (liquidationOrder.getRemainingQuantity() > 0) {
                        orderLifecycleService.cancelOrder(liquidationOrder);
                    }

                    if (executedQuantity == 0) {
                        break;
                    }
                }
            }
        } finally {
            liquidatingUsers.remove(userId);
        }
    }

    private BigDecimal getUnrealizedLoss(Position position, String userCurrency) {
        Stock stock = stockRepository.findById(position.getStockId()).orElseThrow();
        Exchange exchange = exchangeRepository.findById(stock.getExchangeId()).orElseThrow();

        BigDecimal currentPriceInUserCurrency = forexService.convert(stock.getLastTradedPrice(), exchange.getCurrency(), userCurrency);
        BigDecimal unrealizedPnl = currentPriceInUserCurrency.subtract(position.getAverageBuyPrice()).multiply(BigDecimal.valueOf(position.getQuantity()));

        return unrealizedPnl.min(BigDecimal.ZERO);
    }

    private boolean shouldLiquidate(TradingAccount tradingAccount) {
        List<Position> positions = positionRepository.findByUserId(tradingAccount.getUserId());
        String userCurrency = tradingAccount.getBaseCurrency();

        Wallet wallet = walletService.getWalletByUserId(tradingAccount.getUserId());
        BigDecimal totalCashValue = BigDecimal.ZERO;
        for (WalletBucket bucket : wallet.getBuckets()) {
            totalCashValue = totalCashValue.add(forexService.convert(bucket.getBalance(), bucket.getCurrency(), userCurrency));
        }

        BigDecimal totalPositionValue = BigDecimal.ZERO;

        for (Position position : positions) {
            Stock stock = stockRepository.findById(position.getStockId()).orElseThrow();
            Exchange exchange = exchangeRepository.findById(stock.getExchangeId()).orElseThrow();

            BigDecimal currentPriceInUserCurrency = forexService.convert(stock.getLastTradedPrice(), exchange.getCurrency(), userCurrency);
            BigDecimal positionValue = currentPriceInUserCurrency.multiply(BigDecimal.valueOf(position.getQuantity()));

            totalPositionValue = totalPositionValue.add(positionValue);
        }

        BigDecimal equity = totalCashValue.add(totalPositionValue).subtract(tradingAccount.getMarginLoan());

        BigDecimal marginUsed = BigDecimal.ZERO;
        if (totalPositionValue.compareTo(BigDecimal.ZERO) > 0) {
            marginUsed = totalPositionValue.divide(BigDecimal.valueOf(tradingAccount.getLeverage()), 4, RoundingMode.HALF_UP);
        }

        BigDecimal maintenanceMargin = marginUsed.multiply(tradingAccount.getMaintenanceMarginPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));

        return equity.compareTo(maintenanceMargin) < 0;
    }
}