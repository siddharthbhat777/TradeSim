package com.siddharth.tradesim_backend.risk;

import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class RiskServiceTest {
    private RiskService riskService;
    private User user;

    @BeforeEach
    void setup() {
        riskService = new RiskService(null, null, null);

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
}