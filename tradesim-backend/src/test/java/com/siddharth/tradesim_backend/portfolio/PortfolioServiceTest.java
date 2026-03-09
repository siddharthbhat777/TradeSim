package com.siddharth.tradesim_backend.portfolio;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.holding.HoldingRepository;
import com.siddharth.tradesim_backend.holding.model.Holding;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.portfolio.dto.PortfolioResponse;
import com.siddharth.tradesim_backend.portfolio.dto.TradeExecution;
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
    private HoldingRepository holdingRepository;

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

        Holding holding = Holding.builder()
                .userId(userId)
                .stockId(stockId)
                .quantity(10)
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .symbol("AAPL")
                .lastTradedPrice(BigDecimal.valueOf(100))
                .build();

        when(holdingRepository.findByUserId(userId)).thenReturn(List.of(holding));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        PortfolioResponse response = portfolioService.fetchPortfolio(userId);

        assertThat(response.holdings()).hasSize(1);
        assertThat(response.totalValue()).isEqualByComparingTo("1000");
    }

    @Test
    void shouldThrowExceptionWhenStockNotFoundDuringPortfolioFetch() {
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        Holding holding = Holding.builder()
                .userId(userId)
                .stockId(stockId)
                .quantity(10)
                .build();

        when(holdingRepository.findByUserId(userId)).thenReturn(List.of(holding));
        when(stockRepository.findById(stockId)).thenReturn(Optional.empty());

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

        Holding sellerHolding = mock(Holding.class);

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
        when(holdingRepository.findByUserIdAndStockId(sellerId, stockId)).thenReturn(Optional.of(sellerHolding));
        when(holdingRepository.findByUserIdAndStockId(buyerId, stockId)).thenReturn(Optional.empty());
        when(sellerHolding.getQuantity()).thenReturn(5);

        portfolioService.settleTrade(execution);

        verify(buyer).debit(BigDecimal.valueOf(500));
        verify(seller).credit(BigDecimal.valueOf(500));
        verify(sellerHolding).decreaseQuantity(5);
        verify(holdingRepository, times(2)).save(any(Holding.class));
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

        Holding sellerHolding = mock(Holding.class);

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
        when(holdingRepository.findByUserIdAndStockId(sellerId, stockId)).thenReturn(Optional.of(sellerHolding));
        when(holdingRepository.findByUserIdAndStockId(buyerId, stockId)).thenReturn(Optional.empty());
        when(sellerHolding.getQuantity()).thenReturn(10);

        portfolioService.settleTrade(execution);

        verify(buyer).unlockFunds(BigDecimal.valueOf(500));
        verify(buyer).debit(BigDecimal.valueOf(450));
    }

    @Test
    void shouldDeleteSellerHoldingWhenQuantityBecomesZero() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User buyer = mock(User.class);
        User seller = mock(User.class);

        Holding sellerHolding = mock(Holding.class);

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
        when(holdingRepository.findByUserIdAndStockId(sellerId, stockId)).thenReturn(Optional.of(sellerHolding));
        when(holdingRepository.findByUserIdAndStockId(buyerId, stockId)).thenReturn(Optional.empty());
        when(sellerHolding.getQuantity()).thenReturn(0);

        portfolioService.settleTrade(execution);

        verify(holdingRepository).delete(sellerHolding);
    }
}