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

        Stock stock = new Stock();
        stock.setId(stockId);
        stock.setCurrentPrice(new BigDecimal("100"));
        stock.setActive(true);

        Trade trade = new Trade();
        trade.setStockId(stockId);
        trade.setType(Type.BUY);
        trade.setOrderType(OrderType.MARKET);
        trade.setQuantity(10);
        trade.setStatus(Status.PENDING);

        when(stockRepository.findById(stockId))
                .thenReturn(Optional.of(stock));

        when(tradeRepository.save(any(Trade.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(authRepository.save(any(User.class)))
                .thenReturn(user);

        when(holdingRepository.findByUserIdAndStockId(userId, stockId))
                .thenReturn(Optional.empty());

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

        Stock stock = new Stock();
        stock.setId(stockId);
        stock.setCurrentPrice(new BigDecimal("100"));
        stock.setActive(true);

        Trade trade = new Trade();
        trade.setStockId(stockId);
        trade.setType(Type.BUY);
        trade.setOrderType(OrderType.MARKET);
        trade.setQuantity(12);
        trade.setStatus(Status.PENDING);

        when(stockRepository.findById(stockId))
                .thenReturn(Optional.of(stock));

        when(tradeRepository.save(any(Trade.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

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

        Stock stock = new Stock();
        stock.setId(stockId);
        stock.setActive(false);

        Trade trade = new Trade();
        trade.setStockId(stockId);
        trade.setType(Type.BUY);
        trade.setOrderType(OrderType.MARKET);
        trade.setQuantity(12);
        trade.setStatus(Status.PENDING);

        when(stockRepository.findById(stockId))
                .thenReturn(Optional.of(stock));

        when(tradeRepository.save(any(Trade.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        tradeExecutionService.executeTrade(trade, user, stock.getCurrentPrice());

        assertThat(trade.getStatus()).isEqualTo(Status.FAILED);
        assertThat(trade.getPriceAtExecution()).isNull();
        assertThat(user.getBalance()).isEqualTo(new BigDecimal("10000"));
    }

    @Test
    void shouldFailSellTradeWhenHoldingIsInsufficient() {
        UUID stockId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setBalance(new BigDecimal("10000"));

        Stock stock = new Stock();
        stock.setId(stockId);
        stock.setCurrentPrice(new BigDecimal("100"));
        stock.setActive(true);

        Trade trade = new Trade();
        trade.setStockId(stockId);
        trade.setType(Type.SELL);
        trade.setOrderType(OrderType.MARKET);
        trade.setQuantity(12);
        trade.setStatus(Status.PENDING);

        when(stockRepository.findById(stockId))
                .thenReturn(Optional.of(stock));

        when(tradeRepository.save(any(Trade.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(holdingRepository.findByUserIdAndStockId(userId, stockId))
                .thenReturn(Optional.empty());

        tradeExecutionService.executeTrade(trade, user, stock.getCurrentPrice());

        assertThat(trade.getStatus()).isEqualTo(Status.FAILED);
        assertThat(trade.getPriceAtExecution()).isNull();
        assertThat(user.getBalance()).isEqualTo(new BigDecimal("10000"));

        verify(holdingRepository, never()).save(any());
        verify(holdingRepository, never()).delete(any());
    }
}