package com.siddharth.tradesim_backend.order.service;

import com.siddharth.tradesim_backend.auth.repository.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.exchange.ExchangeService;
import com.siddharth.tradesim_backend.exchange.model.Exchange;
import com.siddharth.tradesim_backend.exchange.ExchangeException;
import com.siddharth.tradesim_backend.forex.service.ForexService;
import com.siddharth.tradesim_backend.forex.service.FxFeeService;
import com.siddharth.tradesim_backend.ledger.LedgerService;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.enums.TimeInForce;
import com.siddharth.tradesim_backend.order.OrderException;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.model.dto.OrderRequest;
import com.siddharth.tradesim_backend.order.model.dto.OrderResponse;
import com.siddharth.tradesim_backend.order.orderbook.MatchResult;
import com.siddharth.tradesim_backend.order.orderbook.OrderBook;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookManager;
import com.siddharth.tradesim_backend.order.orderbook.OrderMatchingEngine;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.PositionException;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.risk.service.RiskService;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.StockException;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.stock.service.MarketStateService;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import com.siddharth.tradesim_backend.user.UserException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final AuthRepository authRepository;
    private final StockRepository stockRepository;
    private final ExchangeRepository exchangeRepository;
    private final OrderRepository orderRepository;
    private final PositionRepository positionRepository;
    private final OrderBookManager orderBookManager;
    private final OrderMatchingEngine orderMatchingEngine;
    private final RiskService riskService;
    private final ExchangeService exchangeService;
    private final TradingAccountService tradingAccountService;
    private final LedgerService ledgerService;
    private final OrderLifecycleService orderLifecycleService;
    private final MarketStateService marketStateService;
    private final ForexService forexService;
    private final FxFeeService fxFeeService;

    @Transactional
    public OrderResponse createOrder(UUID userId, @Valid OrderRequest request) {
        User user = authRepository.findById(userId).orElseThrow(() -> UserException.notFound("User not found"));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw UserException.conflict("User account is not active");
        }

        Stock stock = stockRepository.findById(request.stockId()).orElseThrow(() -> StockException.notFound("Stock not found"));

        exchangeService.assertTradingAllowed(stock.getExchangeId());

        if (stock.getStatus() != StockStatus.ACTIVE) {
            throw StockException.conflict("Stock is not active");
        }

        Exchange exchange = exchangeRepository.findById(stock.getExchangeId()).orElseThrow(() -> ExchangeException.notFound("Exchange not found"));

        validateOrderShape(request);

        Instant expiresAt = request.timeInForce() == TimeInForce.DAY ? exchangeService.resolveDayOrderExpiry(stock.getExchangeId()) : null;
        ReentrantLock orderBookLock = orderBookManager.getLock(stock.getId());
        orderBookLock.lock();
        TradingAccount tradingAccount = null;
        Order order;
        MatchResult result;
        try {
            BigDecimal reservationPrice = null;
            if (request.side() == OrderSide.BUY) {
                tradingAccount = tradingAccountService.getTradingAccountByUserIdForUpdate(userId);
                reservationPrice = prepareBuyReservation(tradingAccount, stock, exchange, request);
            } else {
                prepareSellReservation(userId, stock.getId(), request);
            }

            BigDecimal initialBookPrice = request.orderType() == OrderType.LIMIT ? request.limitPrice() : null;

            order = Order.builder()
                    .userId(userId)
                    .stockId(stock.getId())
                    .side(request.side())
                    .orderType(request.orderType())
                    .timeInForce(request.timeInForce())
                    .quantity(request.quantity())
                    .remainingQuantity(request.quantity())
                    .limitPrice(request.limitPrice())
                    .reservationPrice(reservationPrice)
                    .bookPrice(initialBookPrice)
                    .expiresAt(expiresAt)
                    .status(OrderStatus.OPEN)
                    .build();

            orderRepository.save(order);

            if (tradingAccount != null) {
                recordLockLedgerIfRequired(order, tradingAccount, exchange.getCurrency());
            }

            if (order.getBookPrice() != null) {
                orderBookManager.addOrder(order);
            }

            result = orderMatchingEngine.match(order);
            finalizeRemainder(order, stock, result);
        } finally {
            orderBookLock.unlock();
        }

        riskService.checkLiquidation(userId);
        orderRepository.save(order);

        return new OrderResponse(
                order.getId(),
                order.getStockId(),
                order.getSide(),
                order.getOrderType(),
                order.getTimeInForce(),
                order.getStatus(),
                order.getQuantity(),
                order.getRemainingQuantity(),
                order.getLimitPrice(),
                order.getBookPrice(),
                order.getExpiresAt(),
                resolveHaltReason(order, result)
        );
    }

    @Transactional
    public void cancelOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> OrderException.notFound("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw OrderException.forbidden("You are not allowed to cancel this order");
        }

        if (order.getStatus() != OrderStatus.OPEN && order.getStatus() != OrderStatus.PARTIALLY_FILLED) {
            throw OrderException.conflict("Only open or partially filled orders can be cancelled");
        }

        orderLifecycleService.cancelOrder(order);
    }

    private void validateOrderShape(@Valid OrderRequest request) {
        if (request.orderType() == OrderType.LIMIT && request.limitPrice() == null) {
            throw new OrderException("Limit price missing for LIMIT order");
        }

        if (request.orderType() == OrderType.MARKET && request.limitPrice() != null) {
            throw new OrderException("Limit price must be omitted for MARKET order");
        }
    }

    private BigDecimal prepareBuyReservation(TradingAccount tradingAccount, Stock stock, Exchange exchange, @Valid OrderRequest request) {
        return switch (request.orderType()) {
            case LIMIT -> lockLimitBuy(tradingAccount, exchange.getCurrency(), request);
            case MARKET -> prepareMarketBuy(tradingAccount, stock, exchange.getCurrency(), request);
        };
    }

    private void prepareSellReservation(UUID userId, UUID stockId, @Valid OrderRequest request) {
        switch (request.orderType()) {
            case LIMIT -> lockSellPosition(userId, stockId, request.quantity());
            case MARKET -> {
                if (request.timeInForce() == TimeInForce.DAY) {
                    lockSellPosition(userId, stockId, request.quantity());
                } else {
                    validateUserPosition(userId, stockId, request.quantity());
                }
            }
        }
    }

    private BigDecimal lockLimitBuy(TradingAccount tradingAccount, String stockCurrency, @Valid OrderRequest request) {
        BigDecimal blockValueInStockCurrency = request.limitPrice().multiply(BigDecimal.valueOf(request.quantity()));
        BigDecimal requiredMarginInStockCurrency = blockValueInStockCurrency.divide(BigDecimal.valueOf(tradingAccount.getLeverage()), 4, RoundingMode.HALF_UP);

        BigDecimal requiredMarginInAccountCurrency = forexService.convert(requiredMarginInStockCurrency, stockCurrency, tradingAccount.getBaseCurrency());
        BigDecimal fxFee = fxFeeService.calculateConversionFee(tradingAccount.getBaseCurrency(), stockCurrency, requiredMarginInAccountCurrency);
        tradingAccount.lockFunds(requiredMarginInAccountCurrency.add(fxFee));
        return request.limitPrice();
    }

    private BigDecimal prepareMarketBuy(TradingAccount tradingAccount, Stock stock, String stockCurrency, @Valid OrderRequest request) {
        if (request.timeInForce() == TimeInForce.IOC) {
            BigDecimal estimatedCostInStockCurrency = estimateMarketBuyCost(stock.getId(), request.quantity());
            BigDecimal estimatedCostInAccountCurrency = forexService.convert(estimatedCostInStockCurrency, stockCurrency, tradingAccount.getBaseCurrency());
            BigDecimal fxFeeEst = fxFeeService.calculateConversionFee(tradingAccount.getBaseCurrency(), stockCurrency, estimatedCostInAccountCurrency);
            riskService.validateBuyOrder(tradingAccount, estimatedCostInAccountCurrency.add(fxFeeEst));
            return null;
        }

        BigDecimal reservationPrice = calculateProtectedMarketBuyPrice(stock);
        BigDecimal blockValueInStockCurrency = reservationPrice.multiply(BigDecimal.valueOf(request.quantity()));

        BigDecimal orderValueInAccountCurrency = forexService.convert(blockValueInStockCurrency, stockCurrency, tradingAccount.getBaseCurrency());
        BigDecimal totalValueFxFee = fxFeeService.calculateConversionFee(tradingAccount.getBaseCurrency(), stockCurrency, orderValueInAccountCurrency);
        riskService.validateBuyOrder(tradingAccount, orderValueInAccountCurrency.add(totalValueFxFee));

        BigDecimal requiredMarginInStockCurrency = blockValueInStockCurrency.divide(BigDecimal.valueOf(tradingAccount.getLeverage()), 4, RoundingMode.HALF_UP);
        BigDecimal requiredMarginInAccountCurrency = forexService.convert(requiredMarginInStockCurrency, stockCurrency, tradingAccount.getBaseCurrency());
        BigDecimal marginFxFee = fxFeeService.calculateConversionFee(tradingAccount.getBaseCurrency(), stockCurrency, requiredMarginInAccountCurrency);
        tradingAccount.lockFunds(requiredMarginInAccountCurrency.add(marginFxFee));
        return reservationPrice;
    }

    private void lockSellPosition(UUID userId, UUID stockId, int quantity) {
        Position position = positionRepository.findByUserIdAndStockId(userId, stockId).orElseThrow(() -> PositionException.conflict("No shares to sell"));
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
        Position position = positionRepository.findByUserIdAndStockId(userId, stockId).orElseThrow(() -> PositionException.conflict("No shares to sell"));
        if (position.getAvailableQuantity() < quantity) {
            throw PositionException.conflict("Insufficient shares to sell");
        }
    }

    private void recordLockLedgerIfRequired(Order order, TradingAccount tradingAccount, String stockCurrency) {
        if (order.getSide() != OrderSide.BUY || order.getReservationPrice() == null) {
            return;
        }

        BigDecimal blockValueInStockCurrency = order.getReservationPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
        BigDecimal lockedMarginInStockCurrency = blockValueInStockCurrency.divide(BigDecimal.valueOf(tradingAccount.getLeverage()), 4, RoundingMode.HALF_UP);

        BigDecimal lockedMarginInAccountCurrency = forexService.convert(lockedMarginInStockCurrency, stockCurrency, tradingAccount.getBaseCurrency());
        BigDecimal fxFee = fxFeeService.calculateConversionFee(tradingAccount.getBaseCurrency(), stockCurrency, lockedMarginInAccountCurrency);
        BigDecimal totalLock = lockedMarginInAccountCurrency.add(fxFee);

        tradingAccountService.saveTradingAccount(tradingAccount);

        if (order.getOrderType() == OrderType.LIMIT) {
            ledgerService.recordBuyLimitMarginLock(tradingAccount, totalLock, order.getStockId(), order.getId());
            return;
        }

        ledgerService.recordBuyOrderMarginLock(tradingAccount, totalLock, order.getStockId(), order.getId());
    }

    private void finalizeRemainder(Order order, Stock stock, MatchResult result) {
        if (order.getRemainingQuantity() == 0) {
            return;
        }

        if (order.getTimeInForce() == TimeInForce.IOC) {
            orderLifecycleService.cancelOrder(order);
            return;
        }

        if (order.getOrderType() == OrderType.MARKET && order.getBookPrice() == null) {
            BigDecimal bookPrice = resolveRestingBookPrice(order, stock.getId(), result);
            order.assignBookPrice(bookPrice);
            orderRepository.save(order);
            orderBookManager.addOrder(order);
        }
    }

    private BigDecimal resolveRestingBookPrice(Order order, UUID stockId, MatchResult result) {
        BigDecimal candidatePrice = result.lastExecutionPrice() != null
                ? result.lastExecutionPrice()
                : marketStateService.calculateIndicativePrice(stockId);

        if (candidatePrice == null || candidatePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderException("Unable to derive resting price for DAY market order");
        }

        if (order.getSide() == OrderSide.BUY && order.getReservationPrice() != null
                && candidatePrice.compareTo(order.getReservationPrice()) > 0) {
            return order.getReservationPrice();
        }

        return candidatePrice;
    }

    private BigDecimal calculateProtectedMarketBuyPrice(Stock stock) {
        BigDecimal referencePrice = stock.getLastTradedPrice();
        if (referencePrice == null) {
            BigDecimal indicativePrice = marketStateService.calculateIndicativePrice(stock.getId());
            if (indicativePrice == null || indicativePrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new OrderException("Unable to determine reference price for market buy");
            }
            referencePrice = indicativePrice;
        }

        if (stock.getPriceBandPercent() == null) {
            return referencePrice.setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal bandPercent = stock.getPriceBandPercent().divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP);
        return referencePrice.multiply(BigDecimal.ONE.add(bandPercent)).setScale(4, RoundingMode.HALF_UP);
    }

    private String resolveHaltReason(Order order, MatchResult result) {
        if (!result.priceBandHit()) {
            return null;
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return "Price band limit reached. Remaining quantity cancelled.";
        }

        return "Price band limit reached. Remaining quantity pending.";
    }
}