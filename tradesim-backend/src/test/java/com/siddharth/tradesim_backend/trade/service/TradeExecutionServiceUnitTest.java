package com.siddharth.tradesim_backend.trade.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.holding.HoldingRepository;
import com.siddharth.tradesim_backend.holding.model.Holding;
import com.siddharth.tradesim_backend.trade.enums.OrderType;
import com.siddharth.tradesim_backend.trade.model.Trade;
import com.siddharth.tradesim_backend.trade.enums.Status;
import com.siddharth.tradesim_backend.trade.enums.Type;
import com.siddharth.tradesim_backend.trade.TradeRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.stock.StockRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeExecutionServiceUnitTest {

    @Mock
    private StockRepository stockRepository;
    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private AuthRepository authRepository;
    @Mock
    private HoldingRepository holdingRepository;

    @InjectMocks
    private TradeExecutionService tradeExecutionService;

    @Test
    void shouldExecuteBuyTradeSuccessfully() {
        UUID stockId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setBalance(new BigDecimal("10000"));

        Stock stock = Stock.builder()
                .id(stockId)
                .currentPrice(new BigDecimal("100"))
                .active(true)
                .build();

        Trade trade = new Trade();
        trade.setStockId(stockId);
        trade.setType(Type.BUY);
        trade.setOrderType(OrderType.MARKET);
        trade.setQuantity(10);
        trade.setStatus(Status.PENDING);

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authRepository.save(any(User.class))).thenReturn(user);
        when(holdingRepository.findByUserIdAndStockId(userId, stockId)).thenReturn(Optional.empty());

        tradeExecutionService.executeTrade(trade, user, stock.getCurrentPrice());

        assertThat(trade.getStatus()).isEqualTo(Status.EXECUTED);
        assertThat(trade.getPriceAtExecution()).isEqualTo(new BigDecimal("100"));
        assertThat(user.getBalance()).isEqualTo(new BigDecimal("9000"));
    }

    @Test
    void shouldFailBuyTradeWhenBalanceIsInsufficient() {
        UUID stockId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setBalance(new BigDecimal("1000"));

        Stock stock = Stock.builder()
                .id(stockId)
                .currentPrice(new BigDecimal("100"))
                .active(true)
                .build();

        Trade trade = new Trade();
        trade.setStockId(stockId);
        trade.setType(Type.BUY);
        trade.setOrderType(OrderType.MARKET);
        trade.setQuantity(12);
        trade.setStatus(Status.PENDING);

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tradeExecutionService.executeTrade(trade, user, stock.getCurrentPrice());

        assertThat(trade.getStatus()).isEqualTo(Status.FAILED);
        assertThat(trade.getPriceAtExecution()).isNull();
        assertThat(user.getBalance()).isEqualTo(new BigDecimal("1000"));
    }

    @Test
    void shouldFailTradeWhenStockIsInactive() {
        UUID stockId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setBalance(new BigDecimal("10000"));

        Stock stock = Stock.builder()
                .id(stockId)
                .active(false)
                .build();

        Trade trade = new Trade();
        trade.setStockId(stockId);
        trade.setType(Type.BUY);
        trade.setOrderType(OrderType.MARKET);
        trade.setQuantity(12);
        trade.setStatus(Status.PENDING);

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tradeExecutionService.executeTrade(trade, user, stock.getCurrentPrice());

        assertThat(trade.getStatus()).isEqualTo(Status.FAILED);
        assertThat(trade.getPriceAtExecution()).isNull();
        assertThat(user.getBalance()).isEqualTo(new BigDecimal("10000"));
    }

    @Test
    void shouldExecuteSellTradeSuccessfully() {
        UUID stockId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID holdingId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setBalance(new BigDecimal("10000"));

        Holding holding = new Holding();
        holding.setId(holdingId);
        holding.setStockId(stockId);
        holding.setQuantity(12);

        Stock stock = Stock.builder()
                .id(stockId)
                .currentPrice(new BigDecimal("100"))
                .active(true)
                .build();

        Trade trade = new Trade();
        trade.setStockId(stockId);
        trade.setType(Type.SELL);
        trade.setOrderType(OrderType.MARKET);
        trade.setQuantity(8);
        trade.setStatus(Status.PENDING);

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authRepository.save(any(User.class))).thenReturn(user);
        when(holdingRepository.findByUserIdAndStockId(userId, stockId)).thenReturn(Optional.of(holding));

        tradeExecutionService.executeTrade(trade, user, stock.getCurrentPrice());

        assertThat(trade.getStatus()).isEqualTo(Status.EXECUTED);
        assertThat(trade.getPriceAtExecution()).isEqualTo(new BigDecimal("100"));
        assertThat(user.getBalance()).isEqualTo(new BigDecimal("10800"));
        assertThat(holding.getQuantity()).isEqualTo(4);
    }

    @Test
    void shouldFailSellTradeWhenHoldingDoesNotExist() {
        UUID stockId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setBalance(new BigDecimal("10000"));

        Stock stock = Stock.builder()
                .id(stockId)
                .currentPrice(new BigDecimal("100"))
                .active(true)
                .build();

        Trade trade = new Trade();
        trade.setStockId(stockId);
        trade.setType(Type.SELL);
        trade.setOrderType(OrderType.MARKET);
        trade.setQuantity(12);
        trade.setStatus(Status.PENDING);

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(holdingRepository.findByUserIdAndStockId(userId, stockId)).thenReturn(Optional.empty());

        tradeExecutionService.executeTrade(trade, user, stock.getCurrentPrice());

        assertThat(trade.getStatus()).isEqualTo(Status.FAILED);
        assertThat(trade.getPriceAtExecution()).isNull();
        assertThat(user.getBalance()).isEqualTo(new BigDecimal("10000"));

        verify(holdingRepository, never()).save(any());
        verify(holdingRepository, never()).delete(any());
    }

    @Test
    void shouldFailSellTradeWhenHoldingIsInsufficient() {
        UUID stockId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID holdingId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setBalance(new BigDecimal("10000"));

        Holding holding = new Holding();
        holding.setId(holdingId);
        holding.setStockId(stockId);
        holding.setQuantity(10);

        Stock stock = Stock.builder()
                .id(stockId)
                .currentPrice(new BigDecimal("100"))
                .active(true)
                .build();

        Trade trade = new Trade();
        trade.setStockId(stockId);
        trade.setType(Type.SELL);
        trade.setOrderType(OrderType.MARKET);
        trade.setQuantity(12);
        trade.setStatus(Status.PENDING);

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(holdingRepository.findByUserIdAndStockId(userId, stockId)).thenReturn(Optional.of(holding));

        tradeExecutionService.executeTrade(trade, user, stock.getCurrentPrice());

        assertThat(trade.getStatus()).isEqualTo(Status.FAILED);
        assertThat(trade.getPriceAtExecution()).isNull();
        assertThat(user.getBalance()).isEqualTo(new BigDecimal("10000"));

        verify(holdingRepository, never()).save(any());
        verify(holdingRepository, never()).delete(any());
    }

    @Test
    void shouldNotExecuteBuyLimitTradeWhenPriceIsAboveLimit() {
        UUID stockId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setBalance(new BigDecimal("10000"));

        Stock stock = Stock.builder()
                .id(stockId)
                .currentPrice(new BigDecimal("100"))
                .active(true)
                .build();

        Trade trade = new Trade();
        trade.setStockId(stockId);
        trade.setType(Type.BUY);
        trade.setOrderType(OrderType.LIMIT);
        trade.setLimitPrice(new BigDecimal("80"));
        trade.setQuantity(12);
        trade.setStatus(Status.PENDING);

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        tradeExecutionService.executeTrade(trade, user, stock.getCurrentPrice());

        assertThat(trade.getStatus()).isEqualTo(Status.PENDING);
        assertThat(trade.getPriceAtExecution()).isNull();
        assertThat(user.getBalance()).isEqualTo(new BigDecimal("10000"));

        verify(tradeRepository, never()).save(any());
        verify(authRepository, never()).save(any());
    }

    @Test
    void shouldNotExecuteSellLimitTradeWhenPriceIsBelowLimit() {
        UUID stockId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setBalance(new BigDecimal("10000"));

        Stock stock = Stock.builder()
                .id(stockId)
                .currentPrice(new BigDecimal("100"))
                .active(true)
                .build();

        Trade trade = new Trade();
        trade.setStockId(stockId);
        trade.setType(Type.SELL);
        trade.setOrderType(OrderType.LIMIT);
        trade.setLimitPrice(new BigDecimal("120"));
        trade.setQuantity(12);
        trade.setStatus(Status.PENDING);

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        tradeExecutionService.executeTrade(trade, user, stock.getCurrentPrice());

        assertThat(trade.getStatus()).isEqualTo(Status.PENDING);
        assertThat(trade.getPriceAtExecution()).isNull();
        assertThat(user.getBalance()).isEqualTo(new BigDecimal("10000"));

        verify(tradeRepository, never()).save(any());
        verify(authRepository, never()).save(any());
    }

    @Test
    void shouldExecuteBuyLimitTradeWhenPriceIsAtOrBelowLimitWithExistingHolding() {
        UUID stockId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID holdingId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setBalance(new BigDecimal("10000"));

        Holding holding = new Holding();
        holding.setId(holdingId);
        holding.setStockId(stockId);
        holding.setQuantity(12);

        Stock stock = Stock.builder()
                .id(stockId)
                .currentPrice(new BigDecimal("70"))
                .active(true)
                .build();

        Trade trade = new Trade();
        trade.setStockId(stockId);
        trade.setType(Type.BUY);
        trade.setOrderType(OrderType.LIMIT);
        trade.setLimitPrice(new BigDecimal("80"));
        trade.setQuantity(10);
        trade.setStatus(Status.PENDING);

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authRepository.save(any(User.class))).thenReturn(user);
        when(holdingRepository.findByUserIdAndStockId(userId, stockId)).thenReturn(Optional.of(holding));

        tradeExecutionService.executeTrade(trade, user, stock.getCurrentPrice());

        assertThat(trade.getStatus()).isEqualTo(Status.EXECUTED);
        assertThat(trade.getPriceAtExecution()).isEqualTo(stock.getCurrentPrice());
        assertThat(holding.getQuantity()).isEqualTo(22);
        assertThat(user.getBalance()).isEqualTo(new BigDecimal("9300"));
    }

    @Test
    void shouldExecuteBuyLimitTradeWhenPriceIsAtOrBelowLimitWithoutExistingHolding() {
        UUID stockId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setBalance(new BigDecimal("10000"));

        Stock stock = Stock.builder()
                .id(stockId)
                .currentPrice(new BigDecimal("70"))
                .active(true)
                .build();

        Trade trade = new Trade();
        trade.setStockId(stockId);
        trade.setType(Type.BUY);
        trade.setOrderType(OrderType.LIMIT);
        trade.setLimitPrice(new BigDecimal("80"));
        trade.setQuantity(10);
        trade.setStatus(Status.PENDING);

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authRepository.save(any(User.class))).thenReturn(user);
        when(holdingRepository.findByUserIdAndStockId(userId, stockId)).thenReturn(Optional.empty());

        tradeExecutionService.executeTrade(trade, user, stock.getCurrentPrice());

        ArgumentCaptor<Holding> holdingCaptor = ArgumentCaptor.forClass(Holding.class);
        verify(holdingRepository).save(holdingCaptor.capture());

        Holding savedHolding = holdingCaptor.getValue();

        assertThat(savedHolding.getQuantity()).isEqualTo(10);
        assertThat(savedHolding.getStockId()).isEqualTo(stockId);
        assertThat(savedHolding.getUserId()).isEqualTo(userId);
        assertThat(trade.getStatus()).isEqualTo(Status.EXECUTED);
        assertThat(trade.getPriceAtExecution()).isEqualTo(stock.getCurrentPrice());
        assertThat(user.getBalance()).isEqualTo(new BigDecimal("9300"));
    }

    @Test
    void shouldExecuteSellLimitTradeWhenPriceIsAtOrAboveLimit() {
        UUID stockId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID holdingId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setBalance(new BigDecimal("10000"));

        Holding holding = new Holding();
        holding.setId(holdingId);
        holding.setStockId(stockId);
        holding.setQuantity(12);

        Stock stock = Stock.builder()
                .id(stockId)
                .currentPrice(new BigDecimal("90"))
                .active(true)
                .build();

        Trade trade = new Trade();
        trade.setStockId(stockId);
        trade.setType(Type.SELL);
        trade.setOrderType(OrderType.LIMIT);
        trade.setLimitPrice(new BigDecimal("80"));
        trade.setQuantity(10);
        trade.setStatus(Status.PENDING);

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authRepository.save(any(User.class))).thenReturn(user);
        when(holdingRepository.findByUserIdAndStockId(userId, stockId)).thenReturn(Optional.of(holding));

        tradeExecutionService.executeTrade(trade, user, stock.getCurrentPrice());

        assertThat(trade.getStatus()).isEqualTo(Status.EXECUTED);
        assertThat(trade.getPriceAtExecution()).isEqualTo(stock.getCurrentPrice());
        assertThat(holding.getQuantity()).isEqualTo(2);
        assertThat(user.getBalance()).isEqualTo(new BigDecimal("10900"));
    }

    @Test
    void shouldNotExecuteTradeWhenStatusIsNotPending() {
        UUID stockId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setBalance(new BigDecimal("10000"));

        Stock stock = Stock.builder()
                .id(stockId)
                .currentPrice(new BigDecimal("100"))
                .active(true)
                .build();

        Trade trade = new Trade();
        trade.setStockId(stockId);
        trade.setType(Type.BUY);
        trade.setOrderType(OrderType.MARKET);
        trade.setQuantity(10);
        trade.setStatus(Status.EXECUTED);

        tradeExecutionService.executeTrade(trade, user, stock.getCurrentPrice());

        assertThat(trade.getStatus()).isEqualTo(Status.EXECUTED);
        assertThat(trade.getPriceAtExecution()).isNull();
        assertThat(user.getBalance()).isEqualTo(new BigDecimal("10000"));

        verify(tradeRepository, never()).save(any());
        verify(authRepository, never()).save(any());
        verify(holdingRepository, never()).save(any());
        verify(holdingRepository, never()).delete(any());
    }
}