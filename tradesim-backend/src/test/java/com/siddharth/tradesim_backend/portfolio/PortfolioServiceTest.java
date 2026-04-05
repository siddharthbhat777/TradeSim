package com.siddharth.tradesim_backend.portfolio;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.ledger.LedgerService;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.portfolio.model.dto.PortfolioResponse;
import com.siddharth.tradesim_backend.portfolio.model.dto.TradeExecution;
import com.siddharth.tradesim_backend.portfolio.service.PortfolioService;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
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

    @Mock
    private TradingAccountService tradingAccountService;

    @Mock
    private LedgerService ledgerService;

    @InjectMocks
    private PortfolioService portfolioService;

    @Test
    void shouldFetchPortfolioCorrectly() {
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User user = mock(User.class);
        TradingAccount tradingAccount = mock(TradingAccount.class);

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
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);
        when(tradingAccount.calculateEquity(any())).thenReturn(BigDecimal.valueOf(1000));

        PortfolioResponse response = portfolioService.fetchPortfolio(userId);

        assertThat(response.holdings()).hasSize(1);
        assertThat(response.totalValue()).isEqualByComparingTo("1000");
    }

    @Test
    void shouldThrowExceptionWhenStockNotFoundDuringPortfolioFetch() {
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User user = mock(User.class);
        TradingAccount tradingAccount = mock(TradingAccount.class);

        Position position = Position.builder()
                .userId(userId)
                .stockId(stockId)
                .quantity(10)
                .lockedQuantity(0)
                .averageBuyPrice(BigDecimal.valueOf(90))
                .realizedPnl(BigDecimal.ZERO)
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);
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
                UUID.randomUUID(),
                UUID.randomUUID(),
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
        TradingAccount buyerTradingAccount = mock(TradingAccount.class);
        TradingAccount sellerTradingAccount = mock(TradingAccount.class);
        Position sellerPosition = mock(Position.class);

        TradeExecution execution = new TradeExecution(
                buyerId,
                sellerId,
                stockId,
                5,
                BigDecimal.valueOf(100),
                UUID.randomUUID(),
                UUID.randomUUID(),
                OrderType.MARKET,
                OrderType.MARKET,
                null
        );

        when(authRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(authRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(tradingAccountService.getTradingAccountByUserId(buyerId)).thenReturn(buyerTradingAccount);
        when(tradingAccountService.getTradingAccountByUserId(sellerId)).thenReturn(sellerTradingAccount);
        when(positionRepository.findByUserIdAndStockId(sellerId, stockId)).thenReturn(Optional.of(sellerPosition));
        when(positionRepository.findByUserIdAndStockId(buyerId, stockId)).thenReturn(Optional.empty());
        when(sellerPosition.getQuantity()).thenReturn(5);
        when(sellerPosition.getAverageBuyPrice()).thenReturn(BigDecimal.valueOf(90));
        when(buyerTradingAccount.getLeverage()).thenReturn(5);
        when(sellerTradingAccount.getMarginLoan()).thenReturn(BigDecimal.ZERO);

        portfolioService.settleTrade(execution);

        verify(buyerTradingAccount).debit(argThat(amount -> amount.compareTo(BigDecimal.valueOf(100)) == 0));
        verify(buyerTradingAccount).increaseMarginLoan(argThat(amount -> amount.compareTo(BigDecimal.valueOf(400)) == 0));
        verify(sellerTradingAccount).credit(BigDecimal.valueOf(500));
        verify(positionRepository, times(2)).save(any(Position.class));
        verify(tradingAccountService).saveTradingAccount(buyerTradingAccount);
        verify(tradingAccountService).saveTradingAccount(sellerTradingAccount);
        verify(authRepository, never()).save(any());
        verify(ledgerService).recordTradeMarginDebit(eq(buyerTradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(100)) == 0), eq(stockId), any());
        verify(ledgerService).recordMarginLoanIncrease(eq(buyerTradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(400)) == 0), eq(stockId), any());
        verify(ledgerService).recordTradeProceedsCredit(eq(sellerTradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(500)) == 0), eq(stockId), any());
    }

    @Test
    void shouldUnlockFundsForLimitBuyOrder() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User buyer = mock(User.class);
        User seller = mock(User.class);
        TradingAccount buyerTradingAccount = mock(TradingAccount.class);
        TradingAccount sellerTradingAccount = mock(TradingAccount.class);
        Position sellerPosition = mock(Position.class);

        TradeExecution execution = new TradeExecution(
                buyerId,
                sellerId,
                stockId,
                5,
                BigDecimal.valueOf(90),
                UUID.randomUUID(),
                UUID.randomUUID(),
                OrderType.LIMIT,
                OrderType.MARKET,
                BigDecimal.valueOf(100)
        );

        when(authRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(authRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(tradingAccountService.getTradingAccountByUserId(buyerId)).thenReturn(buyerTradingAccount);
        when(tradingAccountService.getTradingAccountByUserId(sellerId)).thenReturn(sellerTradingAccount);
        when(positionRepository.findByUserIdAndStockId(sellerId, stockId)).thenReturn(Optional.of(sellerPosition));
        when(positionRepository.findByUserIdAndStockId(buyerId, stockId)).thenReturn(Optional.empty());
        when(buyerTradingAccount.getLeverage()).thenReturn(10);
        when(sellerTradingAccount.getMarginLoan()).thenReturn(BigDecimal.ZERO);
        when(sellerPosition.getQuantity()).thenReturn(10);
        when(sellerPosition.getAverageBuyPrice()).thenReturn(BigDecimal.valueOf(90));

        portfolioService.settleTrade(execution);

        verify(buyerTradingAccount).unlockFunds(argThat(amount -> amount.compareTo(BigDecimal.valueOf(50)) == 0));
        verify(buyerTradingAccount).debit(argThat(amount -> amount.compareTo(BigDecimal.valueOf(45)) == 0));
        verify(buyerTradingAccount).increaseMarginLoan(argThat(amount -> amount.compareTo(BigDecimal.valueOf(405)) == 0));
        verify(ledgerService).recordBuyLimitMarginUnlock(eq(buyerTradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(50)) == 0), eq(stockId), any());
        verify(ledgerService).recordTradeMarginDebit(eq(buyerTradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(45)) == 0), eq(stockId), any());
        verify(ledgerService).recordMarginLoanIncrease(eq(buyerTradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(405)) == 0), eq(stockId), any());
    }

    @Test
    void shouldDeleteSellerPositionWhenQuantityBecomesZero() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User buyer = mock(User.class);
        User seller = mock(User.class);
        TradingAccount buyerTradingAccount = mock(TradingAccount.class);
        TradingAccount sellerTradingAccount = mock(TradingAccount.class);
        Position sellerPosition = mock(Position.class);

        TradeExecution execution = new TradeExecution(
                buyerId,
                sellerId,
                stockId,
                5,
                BigDecimal.valueOf(100),
                UUID.randomUUID(),
                UUID.randomUUID(),
                OrderType.MARKET,
                OrderType.MARKET,
                null
        );

        when(authRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(authRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(tradingAccountService.getTradingAccountByUserId(buyerId)).thenReturn(buyerTradingAccount);
        when(tradingAccountService.getTradingAccountByUserId(sellerId)).thenReturn(sellerTradingAccount);
        when(positionRepository.findByUserIdAndStockId(sellerId, stockId)).thenReturn(Optional.of(sellerPosition));
        when(positionRepository.findByUserIdAndStockId(buyerId, stockId)).thenReturn(Optional.empty());
        when(sellerPosition.getQuantity()).thenReturn(0);
        when(sellerPosition.getAverageBuyPrice()).thenReturn(BigDecimal.valueOf(90));
        when(buyerTradingAccount.getLeverage()).thenReturn(5);
        when(sellerTradingAccount.getMarginLoan()).thenReturn(BigDecimal.ZERO);

        portfolioService.settleTrade(execution);

        verify(positionRepository).delete(sellerPosition);
    }

    @Test
    void shouldIncreaseMarginLoanAndKeepCashNonNegativeOnBuy() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User buyer = mock(User.class);
        User seller = mock(User.class);

        TradingAccount buyerTradingAccount = TradingAccount.builder()
                .id(UUID.randomUUID())
                .userId(buyerId)
                .balance(BigDecimal.valueOf(100))
                .lockedBalance(BigDecimal.ZERO)
                .marginLoan(BigDecimal.ZERO)
                .leverage(10)
                .maintenanceMarginPercent(BigDecimal.valueOf(25))
                .build();

        TradingAccount sellerTradingAccount = mock(TradingAccount.class);
        when(sellerTradingAccount.getMarginLoan()).thenReturn(BigDecimal.ZERO);

        Position sellerPosition = mock(Position.class);
        when(sellerPosition.getQuantity()).thenReturn(5);
        when(sellerPosition.getAverageBuyPrice()).thenReturn(BigDecimal.valueOf(90));

        TradeExecution execution = new TradeExecution(
                buyerId,
                sellerId,
                stockId,
                5,
                BigDecimal.valueOf(100),
                UUID.randomUUID(),
                UUID.randomUUID(),
                OrderType.MARKET,
                OrderType.MARKET,
                null
        );

        when(authRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(authRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(tradingAccountService.getTradingAccountByUserId(buyerId)).thenReturn(buyerTradingAccount);
        when(tradingAccountService.getTradingAccountByUserId(sellerId)).thenReturn(sellerTradingAccount);
        when(positionRepository.findByUserIdAndStockId(sellerId, stockId)).thenReturn(Optional.of(sellerPosition));
        when(positionRepository.findByUserIdAndStockId(buyerId, stockId)).thenReturn(Optional.empty());

        portfolioService.settleTrade(execution);

        assertThat(buyerTradingAccount.getAvailableBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(buyerTradingAccount.getMarginLoan()).isEqualByComparingTo(BigDecimal.valueOf(450));
    }

    @Test
    void shouldRepayMarginLoanBeforeCreditingCashOnSell() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User buyer = mock(User.class);
        User seller = mock(User.class);

        TradingAccount buyerTradingAccount = mock(TradingAccount.class);
        when(buyerTradingAccount.getLeverage()).thenReturn(5);

        TradingAccount sellerTradingAccount = TradingAccount.builder()
                .id(UUID.randomUUID())
                .userId(sellerId)
                .balance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .marginLoan(BigDecimal.valueOf(300))
                .leverage(5)
                .maintenanceMarginPercent(BigDecimal.valueOf(25))
                .build();

        Position sellerPosition = mock(Position.class);
        when(sellerPosition.getQuantity()).thenReturn(5);
        when(sellerPosition.getAverageBuyPrice()).thenReturn(BigDecimal.valueOf(90));

        TradeExecution execution = new TradeExecution(
                buyerId,
                sellerId,
                stockId,
                5,
                BigDecimal.valueOf(100),
                UUID.randomUUID(),
                UUID.randomUUID(),
                OrderType.MARKET,
                OrderType.MARKET,
                null
        );

        when(authRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(authRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(tradingAccountService.getTradingAccountByUserId(buyerId)).thenReturn(buyerTradingAccount);
        when(tradingAccountService.getTradingAccountByUserId(sellerId)).thenReturn(sellerTradingAccount);
        when(positionRepository.findByUserIdAndStockId(sellerId, stockId)).thenReturn(Optional.of(sellerPosition));
        when(positionRepository.findByUserIdAndStockId(buyerId, stockId)).thenReturn(Optional.empty());

        portfolioService.settleTrade(execution);

        assertThat(sellerTradingAccount.getMarginLoan()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(sellerTradingAccount.getAvailableBalance()).isEqualByComparingTo(BigDecimal.valueOf(200));

        verify(ledgerService).recordMarginLoanRepayment(eq(sellerTradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(300)) == 0), eq(stockId), any());
        verify(ledgerService).recordTradeProceedsCredit(eq(sellerTradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(200)) == 0), eq(stockId), any());
    }
}
