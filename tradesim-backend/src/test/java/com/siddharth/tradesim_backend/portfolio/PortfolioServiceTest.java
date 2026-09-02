package com.siddharth.tradesim_backend.portfolio;

import com.siddharth.tradesim_backend.auth.repository.AuthRepository;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.exchange.model.Exchange;
import com.siddharth.tradesim_backend.forex.service.ForexService;
import com.siddharth.tradesim_backend.forex.service.FxFeeService;
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
import com.siddharth.tradesim_backend.wallet.WalletService;
import com.siddharth.tradesim_backend.wallet.model.Wallet;
import com.siddharth.tradesim_backend.wallet.model.WalletBucket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    private WalletService walletService;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private ExchangeRepository exchangeRepository;

    @Mock
    private ForexService forexService;

    @Mock
    private FxFeeService fxFeeService;

    @InjectMocks
    private PortfolioService portfolioService;

    private void setupForexAndExchangeMocksForSettle(UUID stockId) {
        Stock stock = mock(Stock.class);
        Exchange exchange = mock(Exchange.class);
        UUID exchangeId = UUID.randomUUID();

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(stock.getExchangeId()).thenReturn(exchangeId);

        when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));
        when(exchange.getCurrency()).thenReturn("USD");

        when(forexService.convert(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fxFeeService.calculateConversionFee(any(), any(), any())).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void shouldFetchPortfolioCorrectly() {
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();

        TradingAccount tradingAccount = mock(TradingAccount.class);
        Wallet wallet = Wallet.builder().id(UUID.randomUUID()).userId(userId).buckets(List.of()).build();

        Position position = Position.builder()
                .userId(userId)
                .stockId(stockId)
                .quantity(10)
                .lockedQuantity(0)
                .totalInvested(BigDecimal.valueOf(900))
                .averageBuyPrice(BigDecimal.valueOf(90))
                .realizedPnl(BigDecimal.ZERO)
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .symbol("AAPL")
                .exchangeId(exchangeId)
                .lastTradedPrice(BigDecimal.valueOf(100))
                .build();

        Exchange exchange = mock(Exchange.class);

        when(positionRepository.findByUserId(userId)).thenReturn(List.of(position));
        when(stockRepository.findAllById(List.of(stockId))).thenReturn(List.of(stock));
        when(authRepository.existsById(userId)).thenReturn(true);
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);
        when(walletService.getWalletByUserId(userId)).thenReturn(wallet);

        when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));
        when(exchange.getCurrency()).thenReturn("USD");
        when(tradingAccount.getBaseCurrency()).thenReturn("INR");
        when(tradingAccount.getMarginLoan()).thenReturn(BigDecimal.ZERO);
        when(forexService.convert(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        PortfolioResponse response = portfolioService.fetchPortfolio(userId);

        assertThat(response.holdings()).hasSize(1);
        assertThat(response.totalValue()).isEqualByComparingTo("1000");
    }

    @Test
    void shouldThrowExceptionWhenStockNotFoundDuringPortfolioFetch() {
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        TradingAccount tradingAccount = mock(TradingAccount.class);
        Wallet wallet = Wallet.builder().id(UUID.randomUUID()).userId(userId).buckets(List.of()).build();

        Position position = Position.builder()
                .userId(userId)
                .stockId(stockId)
                .quantity(10)
                .lockedQuantity(0)
                .totalInvested(BigDecimal.valueOf(900))
                .averageBuyPrice(BigDecimal.valueOf(90))
                .realizedPnl(BigDecimal.ZERO)
                .build();

        when(authRepository.existsById(userId)).thenReturn(true);
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);
        when(walletService.getWalletByUserId(userId)).thenReturn(wallet);
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
                null,
                false,
                false,
                "INR",
                "INR"
        );

        assertThrows(BusinessException.class, () -> portfolioService.settleTrade(execution));
    }

    @Test
    void shouldSettleTradeCorrectlyForMarketOrders() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        TradingAccount buyerTradingAccount = mock(TradingAccount.class);
        TradingAccount sellerTradingAccount = mock(TradingAccount.class);
        Position sellerPosition = mock(Position.class);

        Wallet buyerWallet = Wallet.builder().id(UUID.randomUUID()).build();
        WalletBucket buyerBucket = WalletBucket.builder().wallet(buyerWallet).balance(BigDecimal.valueOf(1000)).build();

        Wallet sellerWallet = Wallet.builder().id(UUID.randomUUID()).build();
        WalletBucket sellerBucket = WalletBucket.builder().wallet(sellerWallet).balance(BigDecimal.valueOf(1000)).build();

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
                null,
                false,
                false,
                "INR",
                "INR"
        );

        when(authRepository.existsById(buyerId)).thenReturn(true);
        when(authRepository.existsById(sellerId)).thenReturn(true);
        when(tradingAccountService.getTradingAccountByUserIdForUpdate(buyerId)).thenReturn(buyerTradingAccount);
        when(tradingAccountService.getTradingAccountByUserIdForUpdate(sellerId)).thenReturn(sellerTradingAccount);

        when(walletService.getWalletByUserId(buyerId)).thenReturn(buyerWallet);
        when(walletService.getOrCreateBucketForUpdate(buyerWallet.getId(), "INR")).thenReturn(buyerBucket);
        when(walletService.getWalletByUserId(sellerId)).thenReturn(sellerWallet);
        when(walletService.getOrCreateBucketForUpdate(sellerWallet.getId(), "INR")).thenReturn(sellerBucket);

        when(positionRepository.findUnlockedByUserIdAndStockId(sellerId, stockId)).thenReturn(Optional.of(sellerPosition));
        when(positionRepository.findUnlockedByUserIdAndStockId(buyerId, stockId)).thenReturn(Optional.empty());
        when(sellerPosition.getQuantity()).thenReturn(5);
        when(sellerPosition.getTotalInvested()).thenReturn(BigDecimal.valueOf(450));
        when(buyerTradingAccount.getLeverage()).thenReturn(5);
        when(sellerTradingAccount.getMarginLoan()).thenReturn(BigDecimal.ZERO);

        setupForexAndExchangeMocksForSettle(stockId);

        portfolioService.settleTrade(execution);

        assertThat(buyerBucket.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(900));
        verify(buyerTradingAccount).increaseMarginLoan(argThat(amount -> amount.compareTo(BigDecimal.valueOf(400)) == 0));
        assertThat(sellerBucket.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1500));

        verify(positionRepository, times(2)).save(any(Position.class));
        verify(tradingAccountService).saveTradingAccount(buyerTradingAccount);
        verify(tradingAccountService).saveTradingAccount(sellerTradingAccount);
        verify(ledgerService).recordTradeMarginDebit(eq(buyerBucket), eq(buyerTradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(100)) == 0), eq(stockId), any());
        verify(ledgerService).recordMarginLoanIncrease(eq(buyerBucket), eq(buyerTradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(400)) == 0), eq(stockId), any());
        verify(ledgerService).recordTradeProceedsCredit(eq(sellerBucket), eq(sellerTradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(500)) == 0), eq(stockId), any());
    }

    @Test
    void shouldUnlockFundsForLimitBuyOrder() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        TradingAccount buyerTradingAccount = mock(TradingAccount.class);
        TradingAccount sellerTradingAccount = mock(TradingAccount.class);
        Position sellerPosition = mock(Position.class);

        Wallet buyerWallet = Wallet.builder().id(UUID.randomUUID()).build();
        WalletBucket buyerBucket = WalletBucket.builder().wallet(buyerWallet).balance(BigDecimal.valueOf(1000)).lockedBalance(BigDecimal.valueOf(50)).build();

        Wallet sellerWallet = Wallet.builder().id(UUID.randomUUID()).build();
        WalletBucket sellerBucket = WalletBucket.builder().wallet(sellerWallet).balance(BigDecimal.valueOf(1000)).build();

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
                BigDecimal.valueOf(100),
                true,
                false,
                "INR",
                "INR"
        );

        when(authRepository.existsById(buyerId)).thenReturn(true);
        when(authRepository.existsById(sellerId)).thenReturn(true);
        when(tradingAccountService.getTradingAccountByUserIdForUpdate(buyerId)).thenReturn(buyerTradingAccount);
        when(tradingAccountService.getTradingAccountByUserIdForUpdate(sellerId)).thenReturn(sellerTradingAccount);

        when(walletService.getWalletByUserId(buyerId)).thenReturn(buyerWallet);
        when(walletService.getOrCreateBucketForUpdate(buyerWallet.getId(), "INR")).thenReturn(buyerBucket);
        when(walletService.getWalletByUserId(sellerId)).thenReturn(sellerWallet);
        when(walletService.getOrCreateBucketForUpdate(sellerWallet.getId(), "INR")).thenReturn(sellerBucket);

        when(positionRepository.findUnlockedByUserIdAndStockId(sellerId, stockId)).thenReturn(Optional.of(sellerPosition));
        when(positionRepository.findUnlockedByUserIdAndStockId(buyerId, stockId)).thenReturn(Optional.empty());
        when(buyerTradingAccount.getLeverage()).thenReturn(10);
        when(sellerTradingAccount.getMarginLoan()).thenReturn(BigDecimal.ZERO);
        when(sellerPosition.getQuantity()).thenReturn(10);
        when(sellerPosition.getTotalInvested()).thenReturn(BigDecimal.valueOf(900));

        setupForexAndExchangeMocksForSettle(stockId);

        portfolioService.settleTrade(execution);

        assertThat(buyerBucket.getLockedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(buyerBucket.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(955));

        verify(buyerTradingAccount).increaseMarginLoan(argThat(amount -> amount.compareTo(BigDecimal.valueOf(405)) == 0));
        verify(ledgerService).recordBuyLimitMarginUnlock(eq(buyerBucket), eq(buyerTradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(50)) == 0), eq(stockId), any());
        verify(ledgerService).recordTradeMarginDebit(eq(buyerBucket), eq(buyerTradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(45)) == 0), eq(stockId), any());
        verify(ledgerService).recordMarginLoanIncrease(eq(buyerBucket), eq(buyerTradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(405)) == 0), eq(stockId), any());
    }

    @Test
    void shouldDeleteSellerPositionWhenQuantityBecomesZero() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        TradingAccount buyerTradingAccount = mock(TradingAccount.class);
        TradingAccount sellerTradingAccount = mock(TradingAccount.class);
        Position sellerPosition = mock(Position.class);

        Wallet buyerWallet = Wallet.builder().id(UUID.randomUUID()).build();
        WalletBucket buyerBucket = WalletBucket.builder().wallet(buyerWallet).balance(BigDecimal.valueOf(1000)).build();

        Wallet sellerWallet = Wallet.builder().id(UUID.randomUUID()).build();
        WalletBucket sellerBucket = WalletBucket.builder().wallet(sellerWallet).balance(BigDecimal.valueOf(1000)).build();

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
                null,
                false,
                false,
                "INR",
                "INR"
        );

        when(authRepository.existsById(buyerId)).thenReturn(true);
        when(authRepository.existsById(sellerId)).thenReturn(true);
        when(tradingAccountService.getTradingAccountByUserIdForUpdate(buyerId)).thenReturn(buyerTradingAccount);
        when(tradingAccountService.getTradingAccountByUserIdForUpdate(sellerId)).thenReturn(sellerTradingAccount);

        when(walletService.getWalletByUserId(buyerId)).thenReturn(buyerWallet);
        when(walletService.getOrCreateBucketForUpdate(buyerWallet.getId(), "INR")).thenReturn(buyerBucket);
        when(walletService.getWalletByUserId(sellerId)).thenReturn(sellerWallet);
        when(walletService.getOrCreateBucketForUpdate(sellerWallet.getId(), "INR")).thenReturn(sellerBucket);

        when(positionRepository.findUnlockedByUserIdAndStockId(sellerId, stockId)).thenReturn(Optional.of(sellerPosition));
        when(positionRepository.findUnlockedByUserIdAndStockId(buyerId, stockId)).thenReturn(Optional.empty());

        when(sellerPosition.getQuantity()).thenReturn(5, 0);
        when(sellerPosition.getTotalInvested()).thenReturn(BigDecimal.valueOf(500));

        when(buyerTradingAccount.getLeverage()).thenReturn(5);
        when(sellerTradingAccount.getMarginLoan()).thenReturn(BigDecimal.ZERO);

        setupForexAndExchangeMocksForSettle(stockId);

        portfolioService.settleTrade(execution);

        verify(positionRepository).delete(sellerPosition);
    }

    @Test
    void shouldIncreaseMarginLoanAndKeepCashNonNegativeOnBuy() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        TradingAccount buyerTradingAccount = TradingAccount.builder()
                .id(UUID.randomUUID())
                .userId(buyerId)
                .baseCurrency("INR")
                .marginLoan(BigDecimal.ZERO)
                .leverage(10)
                .maintenanceMarginPercent(BigDecimal.valueOf(25))
                .build();

        TradingAccount sellerTradingAccount = mock(TradingAccount.class);
        when(sellerTradingAccount.getMarginLoan()).thenReturn(BigDecimal.ZERO);

        Wallet buyerWallet = Wallet.builder().id(UUID.randomUUID()).build();
        WalletBucket buyerBucket = WalletBucket.builder().wallet(buyerWallet).balance(BigDecimal.valueOf(100)).build();

        Wallet sellerWallet = Wallet.builder().id(UUID.randomUUID()).build();
        WalletBucket sellerBucket = WalletBucket.builder().wallet(sellerWallet).balance(BigDecimal.valueOf(1000)).build();

        Position sellerPosition = mock(Position.class);
        when(sellerPosition.getQuantity()).thenReturn(5);
        when(sellerPosition.getTotalInvested()).thenReturn(BigDecimal.valueOf(450));

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
                null,
                false,
                false,
                "INR",
                "INR"
        );

        when(authRepository.existsById(buyerId)).thenReturn(true);
        when(authRepository.existsById(sellerId)).thenReturn(true);
        when(tradingAccountService.getTradingAccountByUserIdForUpdate(buyerId)).thenReturn(buyerTradingAccount);
        when(tradingAccountService.getTradingAccountByUserIdForUpdate(sellerId)).thenReturn(sellerTradingAccount);

        when(walletService.getWalletByUserId(buyerId)).thenReturn(buyerWallet);
        when(walletService.getOrCreateBucketForUpdate(buyerWallet.getId(), "INR")).thenReturn(buyerBucket);
        when(walletService.getWalletByUserId(sellerId)).thenReturn(sellerWallet);
        when(walletService.getOrCreateBucketForUpdate(sellerWallet.getId(), "INR")).thenReturn(sellerBucket);

        when(positionRepository.findUnlockedByUserIdAndStockId(sellerId, stockId)).thenReturn(Optional.of(sellerPosition));
        when(positionRepository.findUnlockedByUserIdAndStockId(buyerId, stockId)).thenReturn(Optional.empty());

        setupForexAndExchangeMocksForSettle(stockId);

        portfolioService.settleTrade(execution);

        assertThat(buyerBucket.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(buyerTradingAccount.getMarginLoan()).isEqualByComparingTo(BigDecimal.valueOf(450));
    }

    @Test
    void shouldRepayMarginLoanBeforeCreditingCashOnSell() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        TradingAccount buyerTradingAccount = mock(TradingAccount.class);
        when(buyerTradingAccount.getLeverage()).thenReturn(5);

        TradingAccount sellerTradingAccount = TradingAccount.builder()
                .id(UUID.randomUUID())
                .userId(sellerId)
                .baseCurrency("INR")
                .marginLoan(BigDecimal.valueOf(300))
                .leverage(5)
                .maintenanceMarginPercent(BigDecimal.valueOf(25))
                .build();

        Wallet buyerWallet = Wallet.builder().id(UUID.randomUUID()).build();
        WalletBucket buyerBucket = WalletBucket.builder().wallet(buyerWallet).balance(BigDecimal.valueOf(1000)).build();

        Wallet sellerWallet = Wallet.builder().id(UUID.randomUUID()).build();
        WalletBucket sellerBucket = WalletBucket.builder().wallet(sellerWallet).balance(BigDecimal.ZERO).build();

        Position sellerPosition = mock(Position.class);
        when(sellerPosition.getQuantity()).thenReturn(5);
        when(sellerPosition.getTotalInvested()).thenReturn(BigDecimal.valueOf(450));

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
                null,
                false,
                false,
                "INR",
                "INR"
        );

        when(authRepository.existsById(buyerId)).thenReturn(true);
        when(authRepository.existsById(sellerId)).thenReturn(true);
        when(tradingAccountService.getTradingAccountByUserIdForUpdate(buyerId)).thenReturn(buyerTradingAccount);
        when(tradingAccountService.getTradingAccountByUserIdForUpdate(sellerId)).thenReturn(sellerTradingAccount);

        when(walletService.getWalletByUserId(buyerId)).thenReturn(buyerWallet);
        when(walletService.getOrCreateBucketForUpdate(buyerWallet.getId(), "INR")).thenReturn(buyerBucket);
        when(walletService.getWalletByUserId(sellerId)).thenReturn(sellerWallet);
        when(walletService.getOrCreateBucketForUpdate(sellerWallet.getId(), "INR")).thenReturn(sellerBucket);

        when(positionRepository.findUnlockedByUserIdAndStockId(sellerId, stockId)).thenReturn(Optional.of(sellerPosition));
        when(positionRepository.findUnlockedByUserIdAndStockId(buyerId, stockId)).thenReturn(Optional.empty());

        setupForexAndExchangeMocksForSettle(stockId);

        portfolioService.settleTrade(execution);

        assertThat(sellerTradingAccount.getMarginLoan()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(sellerBucket.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(200));

        verify(ledgerService).recordMarginLoanRepayment(eq(sellerBucket), eq(sellerTradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(300)) == 0), eq(stockId), any());
        verify(ledgerService).recordTradeProceedsCredit(eq(sellerBucket), eq(sellerTradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(200)) == 0), eq(stockId), any());
    }

    @Test
    void shouldUnlockReservedMarginForDayMarketBuy() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        TradingAccount buyerTradingAccount = mock(TradingAccount.class);
        TradingAccount sellerTradingAccount = mock(TradingAccount.class);
        Position sellerPosition = mock(Position.class);

        Wallet buyerWallet = Wallet.builder().id(UUID.randomUUID()).build();
        WalletBucket buyerBucket = WalletBucket.builder().wallet(buyerWallet).balance(BigDecimal.valueOf(1000)).lockedBalance(BigDecimal.valueOf(55)).build();

        Wallet sellerWallet = Wallet.builder().id(UUID.randomUUID()).build();
        WalletBucket sellerBucket = WalletBucket.builder().wallet(sellerWallet).balance(BigDecimal.valueOf(1000)).build();

        TradeExecution execution = new TradeExecution(
                buyerId,
                sellerId,
                stockId,
                5,
                BigDecimal.valueOf(90),
                UUID.randomUUID(),
                UUID.randomUUID(),
                OrderType.MARKET,
                OrderType.MARKET,
                BigDecimal.valueOf(110),
                true,
                false,
                "INR",
                "INR"
        );

        when(authRepository.existsById(buyerId)).thenReturn(true);
        when(authRepository.existsById(sellerId)).thenReturn(true);
        when(tradingAccountService.getTradingAccountByUserIdForUpdate(buyerId)).thenReturn(buyerTradingAccount);
        when(tradingAccountService.getTradingAccountByUserIdForUpdate(sellerId)).thenReturn(sellerTradingAccount);

        when(walletService.getWalletByUserId(buyerId)).thenReturn(buyerWallet);
        when(walletService.getOrCreateBucketForUpdate(buyerWallet.getId(), "INR")).thenReturn(buyerBucket);
        when(walletService.getWalletByUserId(sellerId)).thenReturn(sellerWallet);
        when(walletService.getOrCreateBucketForUpdate(sellerWallet.getId(), "INR")).thenReturn(sellerBucket);

        when(positionRepository.findUnlockedByUserIdAndStockId(sellerId, stockId)).thenReturn(Optional.of(sellerPosition));
        when(positionRepository.findUnlockedByUserIdAndStockId(buyerId, stockId)).thenReturn(Optional.empty());
        when(buyerTradingAccount.getLeverage()).thenReturn(10);
        when(sellerTradingAccount.getMarginLoan()).thenReturn(BigDecimal.ZERO);
        when(sellerPosition.getQuantity()).thenReturn(10);
        when(sellerPosition.getTotalInvested()).thenReturn(BigDecimal.valueOf(900));

        setupForexAndExchangeMocksForSettle(stockId);

        portfolioService.settleTrade(execution);

        assertThat(buyerBucket.getLockedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(ledgerService).recordBuyOrderMarginUnlock(eq(buyerBucket), eq(buyerTradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(55)) == 0), eq(stockId), any());
        assertThat(buyerBucket.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(955));
    }
}