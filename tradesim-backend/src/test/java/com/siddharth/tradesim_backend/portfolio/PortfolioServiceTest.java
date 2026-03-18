package com.siddharth.tradesim_backend.portfolio;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.portfolio.model.dto.PortfolioResponse;
import com.siddharth.tradesim_backend.portfolio.model.dto.TradeExecution;
import com.siddharth.tradesim_backend.portfolio.service.PortfolioService;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private AuthRepository authRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    @Test
    void shouldFetchPortfolioCorrectly() {
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User user = mock(User.class);

        Position position = Position.builder()
                .userId(userId)
                .stockId(stockId)
                .quantity(10)
                .lockedQuantity(0)
                .averageBuyPrice(BigDecimal.valueOf(90))
                .realizedPnl(BigDecimal.ZERO)
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .symbol("AAPL")
                .lastTradedPrice(BigDecimal.valueOf(100))
                .build();

        when(positionRepository.findByUserId(userId)).thenReturn(List.of(position));
        when(stockRepository.findAllById(List.of(stockId))).thenReturn(List.of(stock));
        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.calculateEquity(any())).thenReturn(BigDecimal.valueOf(1000));

        PortfolioResponse response = portfolioService.fetchPortfolio(userId);

        assertThat(response.holdings()).hasSize(1);
        assertThat(response.totalValue()).isEqualByComparingTo("1000");
    }

    @Test
    void shouldThrowExceptionWhenStockNotFoundDuringPortfolioFetch() {
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User user = mock(User.class);

        Position position = Position.builder()
                .userId(userId)
                .stockId(stockId)
                .quantity(10)
                .lockedQuantity(0)
                .averageBuyPrice(BigDecimal.valueOf(90))
                .realizedPnl(BigDecimal.ZERO)
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(positionRepository.findByUserId(userId)).thenReturn(List.of(position));
        when(stockRepository.findAllById(any())).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> portfolioService.fetchPortfolio(userId));
    }

    @Test
    void shouldThrowExceptionForSelfTrading() {
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        TradeExecution execution = new TradeExecution(
                userId,
                userId,
                stockId,
                5,
                BigDecimal.valueOf(100),
                OrderType.MARKET,
                OrderType.MARKET,
                null
        );

        assertThrows(BusinessException.class, () -> portfolioService.settleTrade(execution));
    }

    @Test
    void shouldSettleTradeCorrectlyForMarketOrders() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User buyer = mock(User.class);
        User seller = mock(User.class);

        Position sellerPosition = mock(Position.class);

        TradeExecution execution = new TradeExecution(
                buyerId,
                sellerId,
                stockId,
                5,
                BigDecimal.valueOf(100),
                OrderType.MARKET,
                OrderType.MARKET,
                null
        );

        when(authRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(authRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(positionRepository.findByUserIdAndStockId(sellerId, stockId)).thenReturn(Optional.of(sellerPosition));
        when(positionRepository.findByUserIdAndStockId(buyerId, stockId)).thenReturn(Optional.empty());
        when(sellerPosition.getQuantity()).thenReturn(5);
        when(sellerPosition.getAverageBuyPrice()).thenReturn(BigDecimal.valueOf(90));

        portfolioService.settleTrade(execution);

        verify(buyer).debit(BigDecimal.valueOf(500));
        verify(seller).credit(BigDecimal.valueOf(500));
        verify(sellerPosition).decreaseQuantity(5);
        verify(positionRepository, times(2)).save(any(Position.class));
        verify(authRepository).save(buyer);
        verify(authRepository).save(seller);
    }

    @Test
    void shouldUnlockFundsForLimitBuyOrder() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User buyer = mock(User.class);
        User seller = mock(User.class);

        Position sellerPosition = mock(Position.class);

        TradeExecution execution = new TradeExecution(
                buyerId,
                sellerId,
                stockId,
                5,
                BigDecimal.valueOf(90),
                OrderType.LIMIT,
                OrderType.MARKET,
                BigDecimal.valueOf(100)
        );

        when(authRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(authRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(positionRepository.findByUserIdAndStockId(sellerId, stockId)).thenReturn(Optional.of(sellerPosition));
        when(positionRepository.findByUserIdAndStockId(buyerId, stockId)).thenReturn(Optional.empty());
        when(buyer.getLeverage()).thenReturn(10);
        when(sellerPosition.getQuantity()).thenReturn(10);
        when(sellerPosition.getAverageBuyPrice()).thenReturn(BigDecimal.valueOf(90));

        portfolioService.settleTrade(execution);

        verify(buyer).unlockFunds(argThat(amount -> amount.compareTo(BigDecimal.valueOf(50)) == 0));
        verify(buyer).debit(BigDecimal.valueOf(450));
    }

    @Test
    void shouldDeleteSellerPositionWhenQuantityBecomesZero() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User buyer = mock(User.class);
        User seller = mock(User.class);

        Position sellerPosition = mock(Position.class);

        TradeExecution execution = new TradeExecution(
                buyerId,
                sellerId,
                stockId,
                5,
                BigDecimal.valueOf(100),
                OrderType.MARKET,
                OrderType.MARKET,
                null
        );

        when(authRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(authRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(positionRepository.findByUserIdAndStockId(sellerId, stockId)).thenReturn(Optional.of(sellerPosition));
        when(positionRepository.findByUserIdAndStockId(buyerId, stockId)).thenReturn(Optional.empty());
        when(sellerPosition.getQuantity()).thenReturn(0);
        when(sellerPosition.getAverageBuyPrice()).thenReturn(BigDecimal.valueOf(90));

        portfolioService.settleTrade(execution);

        verify(positionRepository).delete(sellerPosition);
    }
}