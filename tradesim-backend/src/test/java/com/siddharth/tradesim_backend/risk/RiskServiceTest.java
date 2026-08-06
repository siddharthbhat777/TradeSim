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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private LiquidationService liquidationService;

    @Mock
    private ExchangeRepository exchangeRepository;

    @Mock
    private ForexService forexService;

    @InjectMocks
    private RiskService riskService;

    private TradingAccount tradingAccount;

    @BeforeEach
    void setup() {
        tradingAccount = TradingAccount.builder()
                .balance(BigDecimal.valueOf(1000))
                .lockedBalance(BigDecimal.ZERO)
                .marginLoan(BigDecimal.ZERO)
                .leverage(5)
                .maintenanceMarginPercent(BigDecimal.valueOf(25))
                .baseCurrency("INR")
                .build();
    }

    private void mockExchangeAndForex(Stock stock) {
        Exchange exchange = Exchange.builder().currency("USD").build();
        when(exchangeRepository.findById(stock.getExchangeId())).thenReturn(Optional.of(exchange));
        when(forexService.convert(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldAllowBuyWhenMarginSufficient() {
        BigDecimal orderValue = BigDecimal.valueOf(4000);

        assertDoesNotThrow(() -> riskService.validateBuyOrder(tradingAccount, orderValue));
    }

    @Test
    void shouldRejectBuyWhenMarginInsufficient() {
        BigDecimal orderValue = BigDecimal.valueOf(6000);

        assertThrows(BusinessException.class, () -> riskService.validateBuyOrder(tradingAccount, orderValue));
    }

    @Test
    void shouldAllowExactMarginBoundary() {
        BigDecimal orderValue = BigDecimal.valueOf(5000);

        assertDoesNotThrow(() -> riskService.validateBuyOrder(tradingAccount, orderValue));
    }

    @Test
    void shouldLockOnlyMarginNotFullAmount() {
        BigDecimal orderValue = BigDecimal.valueOf(5000);
        BigDecimal requiredMargin = orderValue.divide(BigDecimal.valueOf(tradingAccount.getLeverage()), 4, RoundingMode.HALF_UP);

        tradingAccount.lockFunds(requiredMargin);

        assertEquals(0, tradingAccount.getLockedBalance().compareTo(BigDecimal.valueOf(1000)));
        assertEquals(0, tradingAccount.getAvailableBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldFailWhenMultipleOrdersExceedBalance() {
        tradingAccount.lockFunds(BigDecimal.valueOf(600));
        tradingAccount.lockFunds(BigDecimal.valueOf(300));

        assertThrows(BusinessException.class, () -> tradingAccount.lockFunds(BigDecimal.valueOf(200)));
    }

    @Test
    void shouldCalculateSafeRiskLevel() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .build();

        TradingAccount mockTradingAccount = TradingAccount.builder()
                .userId(userId)
                .baseCurrency("INR")
                .balance(BigDecimal.valueOf(10000))
                .leverage(5)
                .maintenanceMarginPercent(BigDecimal.valueOf(25))
                .build();

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

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(mockTradingAccount);
        when(positionRepository.findByUserId(userId)).thenReturn(List.of(position));
        when(stockRepository.findById(position.getStockId())).thenReturn(Optional.of(stock));

        mockExchangeAndForex(stock);

        RiskResponse response = riskService.getUserRisk(userId);

        assertEquals(RiskLevel.SAFE, response.riskLevel());
        assertFalse(response.isUnderLiquidation());
    }

    @Test
    void shouldTriggerLiquidationWhenEquityBelowMaintenance() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .build();

        TradingAccount mockTradingAccount = TradingAccount.builder()
                .userId(userId)
                .baseCurrency("INR")
                .balance(BigDecimal.ZERO)
                .marginLoan(BigDecimal.valueOf(950))
                .leverage(5)
                .maintenanceMarginPercent(BigDecimal.valueOf(50))
                .build();

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
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(mockTradingAccount);
        when(positionRepository.findByUserId(userId)).thenReturn(List.of(position));
        when(stockRepository.findById(position.getStockId())).thenReturn(Optional.of(stock));

        mockExchangeAndForex(stock);

        riskService.checkLiquidation(userId);

        verify(liquidationService).liquidateUser(userId);
    }

    @Test
    void shouldReturnWarningRiskLevel() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .build();

        TradingAccount mockTradingAccount = TradingAccount.builder()
                .userId(userId)
                .baseCurrency("INR")
                .balance(BigDecimal.ZERO)
                .marginLoan(BigDecimal.valueOf(850))
                .leverage(5)
                .maintenanceMarginPercent(BigDecimal.valueOf(50))
                .build();

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
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(mockTradingAccount);
        when(positionRepository.findByUserId(userId)).thenReturn(List.of(position));
        when(stockRepository.findById(position.getStockId())).thenReturn(Optional.of(stock));

        mockExchangeAndForex(stock);

        RiskResponse response = riskService.getUserRisk(userId);

        assertEquals(RiskLevel.WARNING, response.riskLevel());
    }

    @Test
    void shouldHandleZeroMaintenanceMargin() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .build();

        TradingAccount mockTradingAccount = TradingAccount.builder()
                .userId(userId)
                .baseCurrency("INR")
                .balance(BigDecimal.valueOf(1000))
                .leverage(5)
                .maintenanceMarginPercent(BigDecimal.ZERO)
                .build();

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
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(mockTradingAccount);
        when(positionRepository.findByUserId(userId)).thenReturn(List.of(position));
        when(stockRepository.findById(position.getStockId())).thenReturn(Optional.of(stock));

        mockExchangeAndForex(stock);

        RiskResponse response = riskService.getUserRisk(userId);

        assertNotNull(response);
    }

    @Test
    void shouldReturnSafeWhenNoPositions() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .build();

        TradingAccount mockTradingAccount = TradingAccount.builder()
                .userId(userId)
                .baseCurrency("INR")
                .balance(BigDecimal.valueOf(5000))
                .leverage(5)
                .maintenanceMarginPercent(BigDecimal.valueOf(25))
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(mockTradingAccount);
        when(positionRepository.findByUserId(userId)).thenReturn(List.of());

        RiskResponse response = riskService.getUserRisk(userId);

        assertEquals(RiskLevel.SAFE, response.riskLevel());
        assertFalse(response.isUnderLiquidation());
    }
}