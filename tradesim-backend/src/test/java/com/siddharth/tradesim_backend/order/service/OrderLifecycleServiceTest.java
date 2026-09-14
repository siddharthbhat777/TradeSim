package com.siddharth.tradesim_backend.order.service;

import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.exchange.model.Exchange;
import com.siddharth.tradesim_backend.forex.service.ForexService;
import com.siddharth.tradesim_backend.forex.service.FxFeeService;
import com.siddharth.tradesim_backend.ledger.LedgerService;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.enums.TimeInForce;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookManager;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import com.siddharth.tradesim_backend.wallet.model.Wallet;
import com.siddharth.tradesim_backend.wallet.model.WalletBucket;
import com.siddharth.tradesim_backend.wallet.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderLifecycleServiceTest {
    private OrderLifecycleService service;
    private OrderRepository orderRepository;
    private OrderBookManager orderBookManager;
    private TradingAccountService tradingAccountService;
    private WalletService walletService;
    private PositionRepository positionRepository;
    private LedgerService ledgerService;
    private StockRepository stockRepository;
    private ExchangeRepository exchangeRepository;

    private UUID userId;
    private UUID stockId;

    @BeforeEach
    void setup() {
        orderRepository = mock(OrderRepository.class);
        orderBookManager = mock(OrderBookManager.class);
        tradingAccountService = mock(TradingAccountService.class);
        walletService = mock(WalletService.class);
        positionRepository = mock(PositionRepository.class);
        ledgerService = mock(LedgerService.class);
        stockRepository = mock(StockRepository.class);
        exchangeRepository = mock(ExchangeRepository.class);
        ForexService forexService = mock(ForexService.class);
        FxFeeService fxFeeService = mock(FxFeeService.class);

        service = new OrderLifecycleService(
                orderRepository,
                orderBookManager,
                tradingAccountService,
                walletService,
                positionRepository,
                ledgerService,
                stockRepository,
                exchangeRepository,
                forexService,
                fxFeeService
        );

        userId = UUID.randomUUID();
        stockId = UUID.randomUUID();

        ReentrantLock lock = new ReentrantLock();
        when(orderBookManager.getLock(any())).thenReturn(lock);
        when(forexService.convert(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fxFeeService.calculateConversionFee(any(), any(), any())).thenReturn(BigDecimal.ZERO);
    }

    private Order createOrder(OrderSide side, OrderType type, TimeInForce tif, int qty, BigDecimal limitPrice, BigDecimal reservationPrice) {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .stockId(stockId)
                .side(side)
                .orderType(type)
                .timeInForce(tif)
                .quantity(qty)
                .remainingQuantity(qty)
                .limitPrice(limitPrice)
                .reservationPrice(reservationPrice)
                .bookPrice(limitPrice)
                .status(OrderStatus.OPEN)
                .build();

        ReflectionTestUtils.setField(order, "createdAt", Instant.now());
        return order;
    }

    private void mockBuyerCancellationSetup(TradingAccount tradingAccount, WalletBucket bucket) {
        Stock stock = mock(Stock.class);
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(stock.getExchangeId()).thenReturn(UUID.randomUUID());

        Exchange exchange = mock(Exchange.class);
        when(exchangeRepository.findById(any())).thenReturn(Optional.of(exchange));
        when(exchange.getCurrency()).thenReturn("USD");

        when(tradingAccount.getBaseCurrency()).thenReturn("INR");

        Wallet wallet = Wallet.builder().id(UUID.randomUUID()).build();
        when(walletService.getWalletByUserId(userId)).thenReturn(wallet);
        when(walletService.getBucketForUpdate(wallet.getId(), "INR")).thenReturn(bucket);
    }

    @Test
    void shouldCancelBuyLimitOrderAndUnlockFunds() {
        Order order = createOrder(OrderSide.BUY, OrderType.LIMIT, TimeInForce.DAY, 10, BigDecimal.valueOf(100), BigDecimal.valueOf(100));

        TradingAccount tradingAccount = mock(TradingAccount.class);
        WalletBucket bucket = WalletBucket.builder().balance(BigDecimal.ZERO).lockedBalance(BigDecimal.valueOf(200)).build();

        when(tradingAccountService.getTradingAccountByUserIdForUpdate(userId)).thenReturn(tradingAccount);
        when(tradingAccount.getLeverage()).thenReturn(5);

        mockBuyerCancellationSetup(tradingAccount, bucket);

        service.cancelOrder(order);

        assertEquals(0, bucket.getLockedBalance().compareTo(BigDecimal.ZERO));
        verify(orderBookManager).removeOrder(order);
        verify(orderRepository).save(order);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(ledgerService).recordBuyLimitMarginUnlock(eq(bucket), eq(tradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(200)) == 0), eq(stockId), any());
    }

    @Test
    void shouldCancelBuyDayMarketOrderAndUnlockFunds() {
        Order order = createOrder(OrderSide.BUY, OrderType.MARKET, TimeInForce.DAY, 10, null, BigDecimal.valueOf(110));

        TradingAccount tradingAccount = mock(TradingAccount.class);
        WalletBucket bucket = WalletBucket.builder().balance(BigDecimal.ZERO).lockedBalance(BigDecimal.valueOf(220)).build();

        when(tradingAccountService.getTradingAccountByUserIdForUpdate(userId)).thenReturn(tradingAccount);
        when(tradingAccount.getLeverage()).thenReturn(5);

        mockBuyerCancellationSetup(tradingAccount, bucket);

        service.cancelOrder(order);

        assertEquals(0, bucket.getLockedBalance().compareTo(BigDecimal.ZERO));
        verify(ledgerService).recordBuyOrderMarginUnlock(eq(bucket), eq(tradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(220)) == 0), eq(stockId), any());
    }

    @Test
    void shouldCancelSellLimitOrderAndUnlockShares() {
        Order order = createOrder(OrderSide.SELL, OrderType.LIMIT, TimeInForce.DAY, 5, BigDecimal.valueOf(100), null);
        Position position = mock(Position.class);

        when(positionRepository.findByUserIdAndStockId(userId, stockId)).thenReturn(Optional.of(position));

        service.cancelOrder(order);

        verify(position).unlockShares(5);
        verify(positionRepository).save(position);
        verify(orderRepository).save(order);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void shouldCancelSellDayMarketOrderAndUnlockShares() {
        Order order = createOrder(OrderSide.SELL, OrderType.MARKET, TimeInForce.DAY, 7, null, null);
        Position position = mock(Position.class);

        when(positionRepository.findByUserIdAndStockId(userId, stockId)).thenReturn(Optional.of(position));

        service.cancelOrder(order);

        verify(position).unlockShares(7);
        verify(positionRepository).save(position);
    }

    @Test
    void shouldIgnoreMarketIocBuyUnlock() {
        Order order = createOrder(OrderSide.BUY, OrderType.MARKET, TimeInForce.IOC, 10, null, null);

        service.cancelOrder(order);

        verify(tradingAccountService, never()).getTradingAccountByUserIdForUpdate(any());
        verify(orderBookManager).removeOrder(order);
        verify(orderRepository).save(order);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void shouldNotCancelFilledOrder() {
        Order order = createOrder(OrderSide.BUY, OrderType.LIMIT, TimeInForce.DAY, 10, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        ReflectionTestUtils.setField(order, "status", OrderStatus.FILLED);

        service.cancelOrder(order);

        verify(orderRepository, never()).save(any());
        verify(orderBookManager, never()).removeOrder(any());
    }

    @Test
    void shouldNotCancelAlreadyCancelledOrder() {
        Order order = createOrder(OrderSide.BUY, OrderType.LIMIT, TimeInForce.DAY, 10, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        ReflectionTestUtils.setField(order, "status", OrderStatus.CANCELLED);

        service.cancelOrder(order);

        verify(orderRepository, never()).save(any());
        verify(orderBookManager, never()).removeOrder(any());
    }
}