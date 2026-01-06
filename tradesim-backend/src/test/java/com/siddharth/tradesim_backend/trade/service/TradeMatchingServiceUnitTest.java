package com.siddharth.tradesim_backend.trade.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.trade.TradeRepository;
import com.siddharth.tradesim_backend.trade.enums.OrderType;
import com.siddharth.tradesim_backend.trade.enums.Status;
import com.siddharth.tradesim_backend.trade.enums.Type;
import com.siddharth.tradesim_backend.trade.model.Trade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeMatchingServiceUnitTest {

    @Mock
    private TradeExecutionService tradeExecutionService;
    @Mock
    private AuthRepository authRepository;
    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private TradeMatchingService tradeMatchingService;

    @Test
    void shouldExecuteBuyLimitTradeWhenPriceIsAtOrBelowLimit() {
        UUID tradeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        Trade trade = Trade.builder()
                .id(tradeId)
                .userId(userId)
                .stockId(stockId)
                .type(Type.BUY)
                .orderType(OrderType.LIMIT)
                .limitPrice(new BigDecimal("100"))
                .status(Status.PENDING)
                .build();

        User user = User.builder()
                .id(userId)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .currentPrice(new BigDecimal("90"))
                .status(StockStatus.ACTIVE)
                .build();

        when(tradeRepository.findByStatus(Status.PENDING)).thenReturn(List.of(trade));
        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        tradeMatchingService.processPendingTrades();

        verify(tradeExecutionService).executeTrade(trade, user, stock.getCurrentPrice());
    }

    @Test
    void shouldExecuteSellLimitTradeWhenPriceIsAtOrAboveLimit() {
        UUID tradeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        Trade trade = Trade.builder()
                .id(tradeId)
                .userId(userId)
                .stockId(stockId)
                .type(Type.SELL)
                .orderType(OrderType.LIMIT)
                .limitPrice(new BigDecimal("80"))
                .status(Status.PENDING)
                .build();

        User user = User.builder()
                .id(userId)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .currentPrice(new BigDecimal("90"))
                .status(StockStatus.ACTIVE)
                .build();

        when(tradeRepository.findByStatus(Status.PENDING)).thenReturn(List.of(trade));
        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        tradeMatchingService.processPendingTrades();

        verify(tradeExecutionService).executeTrade(trade, user, stock.getCurrentPrice());
    }

    @Test
    void shouldNotExecuteLimitTradeWhenPriceConditionIsNotMet() {
        UUID tradeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        Trade trade = Trade.builder()
                .id(tradeId)
                .userId(userId)
                .stockId(stockId)
                .type(Type.BUY)
                .orderType(OrderType.LIMIT)
                .limitPrice(new BigDecimal("80"))
                .status(Status.PENDING)
                .build();

        User user = User.builder()
                .id(userId)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .currentPrice(new BigDecimal("100"))
                .status(StockStatus.ACTIVE)
                .build();

        when(tradeRepository.findByStatus(Status.PENDING)).thenReturn(List.of(trade));
        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        tradeMatchingService.processPendingTrades();

        verify(tradeExecutionService, never()).executeTrade(any(), any(), any());
    }

    @Test
    void shouldIgnoreMarketOrdersInMatchingService() {
        UUID tradeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        Trade trade = Trade.builder()
                .id(tradeId)
                .userId(userId)
                .stockId(stockId)
                .type(Type.BUY)
                .orderType(OrderType.MARKET)
                .status(Status.PENDING)
                .build();

        when(tradeRepository.findByStatus(Status.PENDING)).thenReturn(List.of(trade));

        tradeMatchingService.processPendingTrades();

        verify(tradeExecutionService, never()).executeTrade(any(), any(), any());
    }

    @Test
    void shouldNotExecuteTradeWhenUserIsInactive() {
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        Trade trade = Trade.builder()
                .userId(userId)
                .stockId(stockId)
                .type(Type.BUY)
                .orderType(OrderType.LIMIT)
                .limitPrice(new BigDecimal("100"))
                .status(Status.PENDING)
                .build();

        User user = User.builder()
                .id(userId)
                .accountStatus(AccountStatus.DEACTIVATED)
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .currentPrice(new BigDecimal("90"))
                .status(StockStatus.ACTIVE)
                .build();

        when(tradeRepository.findByStatus(Status.PENDING)).thenReturn(List.of(trade));
        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        tradeMatchingService.processPendingTrades();

        verify(tradeExecutionService, never()).executeTrade(any(), any(), any());
    }
}