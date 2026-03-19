package com.siddharth.tradesim_backend.risk;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.risk.dto.RiskResponse;
import com.siddharth.tradesim_backend.risk.enums.RiskLevel;
import com.siddharth.tradesim_backend.risk.service.LiquidationService;
import com.siddharth.tradesim_backend.risk.service.RiskService;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskServiceTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private LiquidationService liquidationService;

    @InjectMocks
    private RiskService riskService;

    private User user;

    @BeforeEach
    void setup() {
        user = User.builder()
                .balance(BigDecimal.valueOf(1000))
                .lockedBalance(BigDecimal.ZERO)
                .leverage(5)
                .maintenanceMarginPercent(BigDecimal.valueOf(25))
                .build();
    }

    @Test
    void shouldAllowBuyWhenMarginSufficient() {
        BigDecimal orderValue = BigDecimal.valueOf(4000);

        assertDoesNotThrow(() -> riskService.validateBuyOrder(user, orderValue));
    }

    @Test
    void shouldRejectBuyWhenMarginInsufficient() {
        BigDecimal orderValue = BigDecimal.valueOf(6000);

        assertThrows(BusinessException.class, () -> riskService.validateBuyOrder(user, orderValue));
    }

    @Test
    void shouldAllowExactMarginBoundary() {
        BigDecimal orderValue = BigDecimal.valueOf(5000);

        assertDoesNotThrow(() -> riskService.validateBuyOrder(user, orderValue));
    }

    @Test
    void shouldLockOnlyMarginNotFullAmount() throws NoSuchFieldException, IllegalAccessException {
        BigDecimal orderValue = BigDecimal.valueOf(5000);
        BigDecimal requiredMargin = orderValue.divide(BigDecimal.valueOf(user.getLeverage()), 4, RoundingMode.HALF_UP);

        user.lockFunds(requiredMargin);

        Field field = User.class.getDeclaredField("lockedBalance");
        field.setAccessible(true);

        BigDecimal lockedBalance = (BigDecimal) field.get(user);

        assertEquals(0, lockedBalance.compareTo(BigDecimal.valueOf(1000)));
    }

    @Test
    void shouldFailWhenMultipleOrdersExceedBalance() {
        user.lockFunds(BigDecimal.valueOf(600));
        user.lockFunds(BigDecimal.valueOf(300));

        assertThrows(BusinessException.class, () -> user.lockFunds(BigDecimal.valueOf(200)));
    }

    @Test
    void shouldCalculateSafeRiskLevel() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
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
                .lastTradedPrice(BigDecimal.valueOf(120))
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(positionRepository.findByUserId(userId)).thenReturn(List.of(position));
        when(stockRepository.findById(position.getStockId())).thenReturn(Optional.of(stock));

        RiskResponse response = riskService.getUserRisk(userId);

        assertEquals(RiskLevel.SAFE, response.riskLevel());
        assertFalse(response.isUnderLiquidation());
    }

    @Test
    void shouldTriggerLiquidationWhenEquityBelowMaintenance() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .balance(BigDecimal.valueOf(100))
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
                .lastTradedPrice(BigDecimal.valueOf(10))
                .build();

        when(positionRepository.findByUserId(userId)).thenReturn(List.of(position));
        when(stockRepository.findById(position.getStockId())).thenReturn(Optional.of(stock));

        riskService.checkLiquidation(user);

        verify(liquidationService).liquidateUser(user);
    }

    @Test
    void shouldReturnWarningRiskLevel() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .balance(BigDecimal.valueOf(200))
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
                .lastTradedPrice(BigDecimal.valueOf(90))
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(positionRepository.findByUserId(userId)).thenReturn(List.of(position));
        when(stockRepository.findById(position.getStockId())).thenReturn(Optional.of(stock));

        RiskResponse response = riskService.getUserRisk(userId);

        assertEquals(RiskLevel.WARNING, response.riskLevel());
    }

    @Test
    void shouldHandleZeroMaintenanceMargin() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
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
                .lastTradedPrice(BigDecimal.valueOf(50))
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(positionRepository.findByUserId(userId)).thenReturn(List.of(position));
        when(stockRepository.findById(position.getStockId())).thenReturn(Optional.of(stock));

        RiskResponse response = riskService.getUserRisk(userId);

        assertNotNull(response);
    }

    @Test
    void shouldReturnSafeWhenNoPositions() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .balance(BigDecimal.valueOf(5000))
                .leverage(5)
                .maintenanceMarginPercent(BigDecimal.valueOf(25))
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(positionRepository.findByUserId(userId)).thenReturn(List.of());

        RiskResponse response = riskService.getUserRisk(userId);

        assertEquals(RiskLevel.SAFE, response.riskLevel());
        assertFalse(response.isUnderLiquidation());
    }
}