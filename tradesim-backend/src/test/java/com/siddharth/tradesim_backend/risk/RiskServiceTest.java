package com.siddharth.tradesim_backend.risk;

import com.siddharth.tradesim_backend.auth.repository.AuthRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.exchange.model.Exchange;
import com.siddharth.tradesim_backend.forex.service.ForexService;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.risk.dto.RiskResponse;
import com.siddharth.tradesim_backend.risk.enums.RiskLevel;
import com.siddharth.tradesim_backend.risk.service.LiquidationService;
import com.siddharth.tradesim_backend.risk.service.RiskService;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import com.siddharth.tradesim_backend.wallet.model.Wallet;
import com.siddharth.tradesim_backend.wallet.model.WalletBucket;
import com.siddharth.tradesim_backend.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskServiceTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private TradingAccountService tradingAccountService;

    @Mock
    private WalletService walletService;

    @Mock
    private LiquidationService liquidationService;

    @Mock
    private ExchangeRepository exchangeRepository;

    @Mock
    private ForexService forexService;

    @InjectMocks
    private RiskService riskService;

    private TradingAccount tradingAccount;
    private Wallet wallet;
    private WalletBucket bucket;

    @BeforeEach
    void setup() {
        UUID userId = UUID.randomUUID();
        tradingAccount = TradingAccount.builder()
                .userId(userId)
                .marginLoan(BigDecimal.ZERO)
                .leverage(5)
                .maintenanceMarginPercent(BigDecimal.valueOf(25))
                .baseCurrency("INR")
                .build();

        wallet = Wallet.builder().id(UUID.randomUUID()).userId(userId).build();
        bucket = WalletBucket.builder().wallet(wallet).currency("INR").balance(BigDecimal.valueOf(1000)).lockedBalance(BigDecimal.ZERO).build();
        wallet.setBuckets(List.of(bucket));
    }

    private void mockExchangeAndForex(Stock stock) {
        Exchange exchange = Exchange.builder().currency("USD").build();
        when(exchangeRepository.findById(stock.getExchangeId())).thenReturn(Optional.of(exchange));
        when(forexService.convert(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldAllowBuyWhenMarginSufficient() {
        BigDecimal orderValue = BigDecimal.valueOf(4000);
        when(walletService.getWalletByUserId(tradingAccount.getUserId())).thenReturn(wallet);
        when(walletService.getBucket(wallet.getId(), "INR")).thenReturn(bucket);

        assertDoesNotThrow(() -> riskService.validateBuyOrder(tradingAccount, orderValue));
    }

    @Test
    void shouldRejectBuyWhenMarginInsufficient() {
        BigDecimal orderValue = BigDecimal.valueOf(6000);
        when(walletService.getWalletByUserId(tradingAccount.getUserId())).thenReturn(wallet);
        when(walletService.getBucket(wallet.getId(), "INR")).thenReturn(bucket);

        assertThrows(BusinessException.class, () -> riskService.validateBuyOrder(tradingAccount, orderValue));
    }

    @Test
    void shouldAllowExactMarginBoundary() {
        BigDecimal orderValue = BigDecimal.valueOf(5000);
        when(walletService.getWalletByUserId(tradingAccount.getUserId())).thenReturn(wallet);
        when(walletService.getBucket(wallet.getId(), "INR")).thenReturn(bucket);

        assertDoesNotThrow(() -> riskService.validateBuyOrder(tradingAccount, orderValue));
    }

    @Test
    void shouldCalculateSafeRiskLevel() {
        UUID userId = tradingAccount.getUserId();

        User user = User.builder().id(userId).build();

        Position position = Position.builder()
                .userId(userId)
                .stockId(UUID.randomUUID())
                .quantity(10)
                .averageBuyPrice(BigDecimal.valueOf(100))
                .build();

        Stock stock = Stock.builder()
                .id(position.getStockId())
                .exchangeId(UUID.randomUUID())
                .lastTradedPrice(BigDecimal.valueOf(120))
                .build();

        bucket.setBalance(BigDecimal.valueOf(10000));

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);
        when(walletService.getWalletByUserId(userId)).thenReturn(wallet);
        when(positionRepository.findByUserId(userId)).thenReturn(List.of(position));
        when(stockRepository.findById(position.getStockId())).thenReturn(Optional.of(stock));

        mockExchangeAndForex(stock);

        RiskResponse response = riskService.getUserRisk(userId);

        assertEquals(RiskLevel.SAFE, response.riskLevel());
        assertFalse(response.isUnderLiquidation());
    }

    @Test
    void shouldTriggerLiquidationWhenEquityBelowMaintenance() {
        UUID userId = tradingAccount.getUserId();

        User user = User.builder().id(userId).build();

        tradingAccount.increaseMarginLoan(BigDecimal.valueOf(950));
        tradingAccount.setMaintenanceMarginPercent(BigDecimal.valueOf(50));
        bucket.setBalance(BigDecimal.ZERO);

        Position position = Position.builder()
                .userId(userId)
                .stockId(UUID.randomUUID())
                .quantity(10)
                .averageBuyPrice(BigDecimal.valueOf(100))
                .build();

        Stock stock = Stock.builder()
                .id(position.getStockId())
                .exchangeId(UUID.randomUUID())
                .lastTradedPrice(BigDecimal.valueOf(100))
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);
        when(walletService.getWalletByUserId(userId)).thenReturn(wallet);
        when(positionRepository.findByUserId(userId)).thenReturn(List.of(position));
        when(stockRepository.findById(position.getStockId())).thenReturn(Optional.of(stock));

        mockExchangeAndForex(stock);

        riskService.checkLiquidation(userId);

        verify(liquidationService).liquidateUser(userId);
    }

    @Test
    void shouldReturnWarningRiskLevel() {
        UUID userId = tradingAccount.getUserId();

        User user = User.builder().id(userId).build();

        tradingAccount.increaseMarginLoan(BigDecimal.valueOf(850));
        tradingAccount.setMaintenanceMarginPercent(BigDecimal.valueOf(50));
        bucket.setBalance(BigDecimal.ZERO);

        Position position = Position.builder()
                .userId(userId)
                .stockId(UUID.randomUUID())
                .quantity(10)
                .averageBuyPrice(BigDecimal.valueOf(100))
                .build();

        Stock stock = Stock.builder()
                .id(position.getStockId())
                .exchangeId(UUID.randomUUID())
                .lastTradedPrice(BigDecimal.valueOf(100))
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);
        when(walletService.getWalletByUserId(userId)).thenReturn(wallet);
        when(positionRepository.findByUserId(userId)).thenReturn(List.of(position));
        when(stockRepository.findById(position.getStockId())).thenReturn(Optional.of(stock));

        mockExchangeAndForex(stock);

        RiskResponse response = riskService.getUserRisk(userId);

        assertEquals(RiskLevel.WARNING, response.riskLevel());
    }

    @Test
    void shouldHandleZeroMaintenanceMargin() {
        UUID userId = tradingAccount.getUserId();

        User user = User.builder().id(userId).build();

        tradingAccount.setMaintenanceMarginPercent(BigDecimal.ZERO);

        Position position = Position.builder()
                .userId(userId)
                .stockId(UUID.randomUUID())
                .quantity(10)
                .averageBuyPrice(BigDecimal.valueOf(100))
                .build();

        Stock stock = Stock.builder()
                .id(position.getStockId())
                .exchangeId(UUID.randomUUID())
                .lastTradedPrice(BigDecimal.valueOf(50))
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);
        when(walletService.getWalletByUserId(userId)).thenReturn(wallet);
        when(positionRepository.findByUserId(userId)).thenReturn(List.of(position));
        when(stockRepository.findById(position.getStockId())).thenReturn(Optional.of(stock));

        mockExchangeAndForex(stock);

        RiskResponse response = riskService.getUserRisk(userId);

        assertNotNull(response);
    }

    @Test
    void shouldReturnSafeWhenNoPositions() {
        UUID userId = tradingAccount.getUserId();

        User user = User.builder().id(userId).build();
        bucket.setBalance(BigDecimal.valueOf(5000));

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);
        when(walletService.getWalletByUserId(userId)).thenReturn(wallet);
        when(positionRepository.findByUserId(userId)).thenReturn(List.of());
        when(forexService.convert(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        RiskResponse response = riskService.getUserRisk(userId);

        assertEquals(RiskLevel.SAFE, response.riskLevel());
        assertFalse(response.isUnderLiquidation());
    }
}