package com.siddharth.tradesim_backend.order.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.model.dto.OrderRequest;
import com.siddharth.tradesim_backend.order.orderbook.MatchResult;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookManager;
import com.siddharth.tradesim_backend.order.orderbook.OrderMatchingEngine;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.exchange.ExchangeService;
import com.siddharth.tradesim_backend.risk.service.RiskService;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    private OrderService orderService;

    private AuthRepository authRepository;
    private StockRepository stockRepository;
    private OrderRepository orderRepository;
    private PositionRepository positionRepository;
    private OrderBookManager orderBookManager;
    private OrderMatchingEngine orderMatchingEngine;
    private RiskService riskService;
    private ExchangeService exchangeService;
    private TradingAccountService tradingAccountService;

    private UUID userId;
    private UUID stockId;
    private UUID exchangeId;

    @BeforeEach
    void setup() {
        authRepository = mock(AuthRepository.class);
        stockRepository = mock(StockRepository.class);
        orderRepository = mock(OrderRepository.class);
        positionRepository = mock(PositionRepository.class);
        orderBookManager = mock(OrderBookManager.class);
        orderMatchingEngine = mock(OrderMatchingEngine.class);
        riskService = mock(RiskService.class);
        exchangeService = mock(ExchangeService.class);
        tradingAccountService = mock(TradingAccountService.class);

        orderService = new OrderService(
                authRepository,
                stockRepository,
                orderRepository,
                positionRepository,
                orderBookManager,
                orderMatchingEngine,
                riskService,
                exchangeService,
                tradingAccountService
        );

        userId = UUID.randomUUID();
        stockId = UUID.randomUUID();
        exchangeId = UUID.randomUUID();

        ReentrantLock lock = new ReentrantLock();
        when(orderBookManager.getLock(any())).thenReturn(lock);
    }

    private OrderRequest createLimitBuyRequest() {
        return new OrderRequest(
                stockId,
                10,
                OrderSide.BUY,
                OrderType.LIMIT,
                BigDecimal.valueOf(100)
        );
    }

    @Test
    void shouldCreateLimitBuyOrder() {
        User user = mock(User.class);
        TradingAccount tradingAccount = mock(TradingAccount.class);
        Stock stock = mock(Stock.class);

        when(user.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);
        when(stock.getStatus()).thenReturn(StockStatus.ACTIVE);
        when(stock.getExchangeId()).thenReturn(exchangeId);
        when(tradingAccount.getLeverage()).thenReturn(5);
        when(stock.getId()).thenReturn(stockId);
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(orderMatchingEngine.match(any())).thenReturn(new MatchResult(false));
        doNothing().when(exchangeService).assertTradingAllowed(exchangeId);

        OrderRequest request = createLimitBuyRequest();

        orderService.createOrder(userId, request);

        verify(tradingAccount).lockFunds(argThat(amount -> amount.compareTo(BigDecimal.valueOf(200)) == 0));
        verify(tradingAccountService).saveTradingAccount(tradingAccount);
        verify(orderRepository).save(any());
        verify(orderBookManager).addOrder(any());
        verify(orderMatchingEngine).match(any());
        verify(riskService).checkLiquidation(userId);
    }

    @Test
    void shouldCreateLimitSellOrder() {
        User user = mock(User.class);
        TradingAccount tradingAccount = mock(TradingAccount.class);
        Stock stock = mock(Stock.class);
        Position position = mock(Position.class);

        when(user.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);
        when(stock.getStatus()).thenReturn(StockStatus.ACTIVE);
        when(stock.getExchangeId()).thenReturn(exchangeId);
        when(stock.getId()).thenReturn(stockId);
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(positionRepository.findByUserIdAndStockId(userId, stockId)).thenReturn(Optional.of(position));
        when(orderMatchingEngine.match(any())).thenReturn(new MatchResult(false));
        doNothing().when(exchangeService).assertTradingAllowed(exchangeId);

        OrderRequest request = new OrderRequest(
                stockId,
                5,
                OrderSide.SELL,
                OrderType.LIMIT,
                BigDecimal.valueOf(100)
        );

        orderService.createOrder(userId, request);

        verify(position).lockShares(5);
        verify(positionRepository).save(position);
    }

    @Test
    void shouldRejectInactiveStock() {
        User user = mock(User.class);
        TradingAccount tradingAccount = mock(TradingAccount.class);
        Stock stock = mock(Stock.class);

        when(user.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);
        when(stock.getStatus()).thenReturn(StockStatus.HALTED);
        when(stock.getExchangeId()).thenReturn(exchangeId);
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        doNothing().when(exchangeService).assertTradingAllowed(exchangeId);

        OrderRequest request = createLimitBuyRequest();

        assertThrows(BusinessException.class, () -> orderService.createOrder(userId, request));
    }

    @Test
    void shouldRejectLimitOrderWithoutLimitPrice() {
        User user = mock(User.class);
        TradingAccount tradingAccount = mock(TradingAccount.class);
        Stock stock = mock(Stock.class);

        when(user.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);
        when(stock.getStatus()).thenReturn(StockStatus.ACTIVE);
        when(stock.getExchangeId()).thenReturn(exchangeId);
        when(stock.getId()).thenReturn(stockId);
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        doNothing().when(exchangeService).assertTradingAllowed(exchangeId);

        OrderRequest request = new OrderRequest(
                stockId,
                10,
                OrderSide.BUY,
                OrderType.LIMIT,
                null
        );

        assertThrows(BusinessException.class, () -> orderService.createOrder(userId, request));
    }
}