package com.siddharth.tradesim_backend.order.service;

import com.siddharth.tradesim_backend.auth.repository.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.exchange.ExchangeService;
import com.siddharth.tradesim_backend.exchange.model.Exchange;
import com.siddharth.tradesim_backend.forex.service.ForexService;
import com.siddharth.tradesim_backend.forex.service.FxFeeService;
import com.siddharth.tradesim_backend.ledger.LedgerService;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.enums.TimeInForce;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.model.dto.OrderRequest;
import com.siddharth.tradesim_backend.order.model.dto.OrderResponse;
import com.siddharth.tradesim_backend.order.orderbook.MatchResult;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookManager;
import com.siddharth.tradesim_backend.order.orderbook.OrderMatchingEngine;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.risk.service.RiskService;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.stock.service.MarketStateService;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {
    private OrderService orderService;

    private AuthRepository authRepository;
    private StockRepository stockRepository;
    private ExchangeRepository exchangeRepository;
    private OrderRepository orderRepository;
    private PositionRepository positionRepository;
    private OrderBookManager orderBookManager;
    private OrderMatchingEngine orderMatchingEngine;
    private RiskService riskService;
    private ExchangeService exchangeService;
    private TradingAccountService tradingAccountService;
    private LedgerService ledgerService;
    private OrderLifecycleService orderLifecycleService;
    private MarketStateService marketStateService;
    private ForexService forexService;

    private UUID userId;
    private UUID stockId;
    private UUID exchangeId;

    @BeforeEach
    void setup() {
        authRepository = mock(AuthRepository.class);
        stockRepository = mock(StockRepository.class);
        exchangeRepository = mock(ExchangeRepository.class);
        orderRepository = mock(OrderRepository.class);
        positionRepository = mock(PositionRepository.class);
        orderBookManager = mock(OrderBookManager.class);
        orderMatchingEngine = mock(OrderMatchingEngine.class);
        riskService = mock(RiskService.class);
        exchangeService = mock(ExchangeService.class);
        tradingAccountService = mock(TradingAccountService.class);
        ledgerService = mock(LedgerService.class);
        orderLifecycleService = mock(OrderLifecycleService.class);
        marketStateService = mock(MarketStateService.class);
        forexService = mock(ForexService.class);
        FxFeeService fxFeeService = mock(FxFeeService.class);

        orderService = new OrderService(
                authRepository,
                stockRepository,
                exchangeRepository,
                orderRepository,
                positionRepository,
                orderBookManager,
                orderMatchingEngine,
                riskService,
                exchangeService,
                tradingAccountService,
                ledgerService,
                orderLifecycleService,
                marketStateService,
                forexService,
                fxFeeService
        );

        userId = UUID.randomUUID();
        stockId = UUID.randomUUID();
        exchangeId = UUID.randomUUID();

        ReentrantLock lock = new ReentrantLock();
        when(orderBookManager.getLock(any())).thenReturn(lock);
        when(fxFeeService.calculateConversionFee(any(), any(), any())).thenReturn(BigDecimal.ZERO);
    }

    private void mockActiveUserAndStock(TradingAccount tradingAccount, Stock stock) {
        User user = mock(User.class);
        when(user.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
        when(authRepository.findById(userId)).thenReturn(Optional.of(user));

        when(tradingAccountService.getTradingAccountByUserIdForUpdate(userId)).thenReturn(tradingAccount);
        when(tradingAccount.getBaseCurrency()).thenReturn("INR");

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(stock.getStatus()).thenReturn(StockStatus.ACTIVE);
        when(stock.getExchangeId()).thenReturn(exchangeId);
        when(stock.getId()).thenReturn(stockId);
        doNothing().when(exchangeService).assertTradingAllowed(exchangeId);

        Exchange exchange = mock(Exchange.class);
        when(exchange.getCurrency()).thenReturn("USD");
        when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));

        when(forexService.convert(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private OrderRequest createLimitBuyDayRequest() {
        return new OrderRequest(
                stockId,
                10,
                OrderSide.BUY,
                OrderType.LIMIT,
                TimeInForce.DAY,
                BigDecimal.valueOf(100)
        );
    }

    @Test
    void shouldCreateLimitBuyDayOrder() {
        TradingAccount tradingAccount = mock(TradingAccount.class);
        Stock stock = mock(Stock.class);
        Instant expiresAt = Instant.parse("2026-04-12T10:00:00Z");

        mockActiveUserAndStock(tradingAccount, stock);
        when(tradingAccount.getLeverage()).thenReturn(5);
        when(exchangeService.resolveDayOrderExpiry(exchangeId)).thenReturn(expiresAt);
        when(orderMatchingEngine.match(any())).thenReturn(new MatchResult(false, false, null));

        OrderRequest request = createLimitBuyDayRequest();
        OrderResponse response = orderService.createOrder(userId, request);

        verify(tradingAccount).lockFunds(argThat(amount -> amount.compareTo(BigDecimal.valueOf(200)) == 0));
        verify(tradingAccountService).saveTradingAccount(tradingAccount);
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(orderBookManager).addOrder(any(Order.class));
        verify(orderMatchingEngine).match(any(Order.class));
        verify(ledgerService).recordBuyLimitMarginLock(eq(tradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(200)) == 0), eq(stockId), any());
        verify(riskService).checkLiquidation(userId);

        assertEquals(TimeInForce.DAY, response.timeInForce());
        assertEquals(expiresAt, response.expiresAt());
        assertEquals(BigDecimal.valueOf(100), response.bookPrice());
    }

    @Test
    void shouldCancelLimitIocRemainderAfterPartialFill() {
        TradingAccount tradingAccount = mock(TradingAccount.class);
        Stock stock = mock(Stock.class);

        mockActiveUserAndStock(tradingAccount, stock);
        when(tradingAccount.getLeverage()).thenReturn(5);
        when(orderMatchingEngine.match(any())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.execute(4);
            return new MatchResult(false, true, BigDecimal.valueOf(100));
        });

        OrderRequest request = new OrderRequest(
                stockId,
                10,
                OrderSide.BUY,
                OrderType.LIMIT,
                TimeInForce.IOC,
                BigDecimal.valueOf(100)
        );

        orderService.createOrder(userId, request);

        verify(orderBookManager).addOrder(any(Order.class));
        verify(orderLifecycleService).cancelOrder(argThat(order ->
                order.getOrderType() == OrderType.LIMIT
                        && order.getTimeInForce() == TimeInForce.IOC
                        && order.getRemainingQuantity() == 6
        ));
    }

    @Test
    void shouldConvertMarketDaySellRemainderToRestingOrder() {
        TradingAccount tradingAccount = mock(TradingAccount.class);
        Stock stock = mock(Stock.class);
        Position position = mock(Position.class);
        Instant expiresAt = Instant.parse("2026-04-12T10:00:00Z");

        mockActiveUserAndStock(tradingAccount, stock);
        when(positionRepository.findByUserIdAndStockId(userId, stockId)).thenReturn(Optional.of(position));
        when(exchangeService.resolveDayOrderExpiry(exchangeId)).thenReturn(expiresAt);
        when(orderMatchingEngine.match(any())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.execute(40);
            return new MatchResult(false, true, BigDecimal.valueOf(100));
        });

        OrderRequest request = new OrderRequest(
                stockId,
                100,
                OrderSide.SELL,
                OrderType.MARKET,
                TimeInForce.DAY,
                null
        );

        OrderResponse response = orderService.createOrder(userId, request);

        verify(position).lockShares(100);
        verify(positionRepository).save(position);
        verify(orderBookManager).addOrder(argThat(order ->
                order.getOrderType() == OrderType.MARKET
                        && order.getTimeInForce() == TimeInForce.DAY
                        && order.getBookPrice() != null
                        && order.getBookPrice().compareTo(BigDecimal.valueOf(100)) == 0
                        && order.getRemainingQuantity() == 60
        ));

        assertEquals(TimeInForce.DAY, response.timeInForce());
        assertEquals(0, response.bookPrice().compareTo(BigDecimal.valueOf(100)));
        assertEquals(expiresAt, response.expiresAt());
    }

    @Test
    void shouldLockProtectedMarginForMarketDayBuy() {
        TradingAccount tradingAccount = mock(TradingAccount.class);
        Stock stock = mock(Stock.class);
        Instant expiresAt = Instant.parse("2026-04-12T10:00:00Z");

        mockActiveUserAndStock(tradingAccount, stock);
        when(tradingAccount.getLeverage()).thenReturn(5);
        when(stock.getLastTradedPrice()).thenReturn(BigDecimal.valueOf(100));
        when(stock.getPriceBandPercent()).thenReturn(BigDecimal.TEN);
        when(exchangeService.resolveDayOrderExpiry(exchangeId)).thenReturn(expiresAt);
        when(orderMatchingEngine.match(any())).thenReturn(new MatchResult(false, false, null));
        when(marketStateService.calculateIndicativePrice(stockId)).thenReturn(BigDecimal.valueOf(101));

        OrderRequest request = new OrderRequest(
                stockId,
                100,
                OrderSide.BUY,
                OrderType.MARKET,
                TimeInForce.DAY,
                null
        );

        orderService.createOrder(userId, request);

        verify(tradingAccount).lockFunds(argThat(amount -> amount.compareTo(BigDecimal.valueOf(2200)) == 0));
        verify(tradingAccountService).saveTradingAccount(tradingAccount);
        verify(ledgerService).recordBuyOrderMarginLock(eq(tradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(2200)) == 0), eq(stockId), any());
        verify(marketStateService).calculateIndicativePrice(stockId);
        verify(orderBookManager).addOrder(argThat(order ->
                order.getOrderType() == OrderType.MARKET
                        && order.getTimeInForce() == TimeInForce.DAY
                        && order.getBookPrice() != null
                        && order.getBookPrice().compareTo(BigDecimal.valueOf(101)) == 0
        ));
    }

    @Test
    void shouldRejectLimitOrderWithoutLimitPrice() {
        TradingAccount tradingAccount = mock(TradingAccount.class);
        Stock stock = mock(Stock.class);

        mockActiveUserAndStock(tradingAccount, stock);

        OrderRequest request = new OrderRequest(
                stockId,
                10,
                OrderSide.BUY,
                OrderType.LIMIT,
                TimeInForce.DAY,
                null
        );

        assertThrows(BusinessException.class, () -> orderService.createOrder(userId, request));
    }

    @Test
    void shouldRejectMarketOrderWithLimitPrice() {
        TradingAccount tradingAccount = mock(TradingAccount.class);
        Stock stock = mock(Stock.class);

        mockActiveUserAndStock(tradingAccount, stock);

        OrderRequest request = new OrderRequest(
                stockId,
                10,
                OrderSide.BUY,
                OrderType.MARKET,
                TimeInForce.IOC,
                BigDecimal.valueOf(100)
        );

        assertThrows(BusinessException.class, () -> orderService.createOrder(userId, request));
    }
}