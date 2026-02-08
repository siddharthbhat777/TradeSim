package com.siddharth.tradesim_backend.order.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.order.repository.TradeRepository;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.enums.Status;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.exceptions.OrderException;
import com.siddharth.tradesim_backend.order.model.Trade;
import com.siddharth.tradesim_backend.order.model.dto.TradeRequest;
import com.siddharth.tradesim_backend.order.model.dto.TradeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TradeServiceTest {

    @Mock
    private StockRepository stockRepository;
    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private AuthRepository authRepository;
    @Mock
    private TradeExecutionService tradeExecutionService;

    @InjectMocks
    private TradeService tradeService;

    @Test
    void shouldPlaceBuyMarketOrderAndDelegateExecution() {
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .balance(new BigDecimal("10000"))
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .symbol("AAPL")
                .currentPrice(new BigDecimal("100"))
                .status(StockStatus.ACTIVE)
                .build();

        TradeRequest request = new TradeRequest();
        ReflectionTestUtils.setField(request, "stockId", stockId);
        ReflectionTestUtils.setField(request, "quantity", 10);
        ReflectionTestUtils.setField(request, "type", OrderSide.BUY);
        ReflectionTestUtils.setField(request, "orderType", OrderType.MARKET);
        ReflectionTestUtils.setField(request, "limitPrice", null);

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TradeResponse response = tradeService.placeOrder(userId, request);

        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeExecutionService).executeTrade(tradeCaptor.capture(), eq(user), eq(stock.getCurrentPrice()));

        assertThat(tradeCaptor.getValue().getStatus()).isEqualTo(Status.PENDING);
        assertThat(response.stockId()).isEqualTo(stockId);
        assertThat(response.type()).isEqualTo(OrderSide.BUY);
        assertThat(response.orderType()).isEqualTo(OrderType.MARKET);
        assertThat(response.quantity()).isEqualTo(10);
        assertThat(response.status()).isEqualTo(Status.PENDING);

        verify(tradeExecutionService).executeTrade(any(Trade.class), eq(user), eq(stock.getCurrentPrice()));
    }

    @Test
    void shouldPlaceBuyMarketOrderEvenWhenBalanceIsInsufficient() {
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .balance(new BigDecimal("500"))
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .symbol("AAPL")
                .currentPrice(new BigDecimal("100"))
                .status(StockStatus.ACTIVE)
                .build();

        TradeRequest request = new TradeRequest();
        ReflectionTestUtils.setField(request, "stockId", stockId);
        ReflectionTestUtils.setField(request, "quantity", 10);
        ReflectionTestUtils.setField(request, "type", OrderSide.BUY);
        ReflectionTestUtils.setField(request, "orderType", OrderType.MARKET);
        ReflectionTestUtils.setField(request, "limitPrice", null);

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TradeResponse response = tradeService.placeOrder(userId, request);

        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeExecutionService).executeTrade(tradeCaptor.capture(), eq(user), eq(stock.getCurrentPrice()));

        assertThat(tradeCaptor.getValue().getStatus()).isEqualTo(Status.PENDING);
        assertThat(response.stockId()).isEqualTo(stockId);
        assertThat(response.quantity()).isEqualTo(10);
        assertThat(response.type()).isEqualTo(OrderSide.BUY);
        assertThat(response.status()).isEqualTo(Status.PENDING);

        verify(tradeExecutionService).executeTrade(any(Trade.class), eq(user), eq(stock.getCurrentPrice()));
    }

    @Test
    void shouldNotExecuteLimitBuyOrderWhenPriceIsAboveLimit() {
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .balance(new BigDecimal("10000"))
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .symbol("AAPL")
                .currentPrice(new BigDecimal("100"))
                .build();

        TradeRequest request = new TradeRequest();
        ReflectionTestUtils.setField(request, "stockId", stockId);
        ReflectionTestUtils.setField(request, "quantity", 10);
        ReflectionTestUtils.setField(request, "type", OrderSide.BUY);
        ReflectionTestUtils.setField(request, "orderType", OrderType.LIMIT);
        ReflectionTestUtils.setField(request, "limitPrice", new BigDecimal("80"));

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TradeResponse response = tradeService.placeOrder(userId, request);

        assertThat(response.status()).isEqualTo(Status.PENDING);
        assertThat(response.priceAtExecution()).isNull();
        assertThat(response.totalAmount()).isNull();

        verify(tradeExecutionService, never()).executeTrade(any(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        TradeRequest request = new TradeRequest();
        ReflectionTestUtils.setField(request, "stockId", stockId);
        ReflectionTestUtils.setField(request, "quantity", 10);
        ReflectionTestUtils.setField(request, "type", OrderSide.BUY);
        ReflectionTestUtils.setField(request, "orderType", OrderType.MARKET);

        when(authRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeService.placeOrder(userId, request)).isInstanceOf(BusinessException.class).hasMessageContaining("User not found");

        verify(stockRepository, never()).findById(any());
        verify(tradeRepository, never()).save(any());
        verify(tradeExecutionService, never()).executeTrade(any(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenStockNotFound() {
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .balance(new BigDecimal("10000"))
                .build();

        TradeRequest request = new TradeRequest();
        ReflectionTestUtils.setField(request, "stockId", stockId);
        ReflectionTestUtils.setField(request, "quantity", 10);
        ReflectionTestUtils.setField(request, "type", OrderSide.BUY);
        ReflectionTestUtils.setField(request, "orderType", OrderType.MARKET);

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(stockRepository.findById(stockId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeService.placeOrder(userId, request)).isInstanceOf(BusinessException.class).hasMessageContaining("Stock not found");

        verify(tradeRepository, never()).save(any());
        verify(tradeExecutionService, never()).executeTrade(any(), any(), any());
    }

    @Test
    void shouldCancelPendingTradeSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID tradeId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .balance(new BigDecimal("10000"))
                .build();

        Trade trade = Trade.builder()
                .id(tradeId)
                .userId(userId)
                .stockId(stockId)
                .status(Status.PENDING)
                .type(OrderSide.BUY)
                .orderType(OrderType.MARKET)
                .quantity(10)
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .symbol("AAPL")
                .currentPrice(new BigDecimal("100"))
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tradeRepository.findById(tradeId)).thenReturn(Optional.of(trade));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TradeResponse response = tradeService.cancelTrade(tradeId, userId);

        assertThat(trade.getStatus()).isEqualTo(Status.CANCELLED);
        assertThat(response.status()).isEqualTo(Status.CANCELLED);

        verify(tradeRepository).save(trade);
    }

    @Test
    void shouldThrowExceptionWhenUserTriesToCancelOthersTrade() {
        UUID ownerUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID tradeId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User otherUser = User.builder()
                .id(otherUserId)
                .build();

        Trade trade = Trade.builder()
                .id(tradeId)
                .userId(ownerUserId)
                .stockId(stockId)
                .status(Status.PENDING)
                .build();

        when(authRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
        when(tradeRepository.findById(tradeId)).thenReturn(Optional.of(trade));

        assertThatThrownBy(() -> tradeService.cancelTrade(tradeId, otherUserId)).isInstanceOf(OrderException.class);

        verify(tradeRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenTradeIsNotPending() {
        UUID userId = UUID.randomUUID();
        UUID tradeId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .build();

        Trade trade = Trade.builder()
                .id(tradeId)
                .userId(userId)
                .stockId(stockId)
                .status(Status.EXECUTED)
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tradeRepository.findById(tradeId)).thenReturn(Optional.of(trade));

        assertThatThrownBy(() -> tradeService.cancelTrade(tradeId, userId)).isInstanceOf(OrderException.class);

        verify(tradeRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenTradeNotFound() {
        UUID userId = UUID.randomUUID();
        UUID tradeId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tradeRepository.findById(tradeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeService.cancelTrade(tradeId, userId)).isInstanceOf(BusinessException.class);

        verify(tradeRepository, never()).save(any());
        verify(stockRepository, never()).findById(any());
    }
}