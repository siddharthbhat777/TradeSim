package com.siddharth.tradesim_backend.position;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.position.model.Position;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PositionTest {

    @Test
    void shouldAddInvestmentCorrectly() {
        Position position = Position.builder()
                .quantity(10)
                .lockedQuantity(0)
                .averageBuyPrice(BigDecimal.valueOf(100))
                .totalInvested(BigDecimal.valueOf(1000))
                .realizedPnl(BigDecimal.ZERO)
                .build();

        position.addInvestment(BigDecimal.valueOf(2000), 10);

        assertThat(position.getQuantity()).isEqualTo(20);
        assertThat(position.getTotalInvested()).isEqualByComparingTo("3000");
        assertThat(position.getAverageBuyPrice()).isEqualByComparingTo("150.0000");
    }

    @Test
    void shouldLockSharesCorrectly() {
        Position position = Position.builder()
                .quantity(10)
                .lockedQuantity(0)
                .averageBuyPrice(BigDecimal.valueOf(100))
                .totalInvested(BigDecimal.valueOf(1000))
                .realizedPnl(BigDecimal.ZERO)
                .build();

        position.lockShares(5);

        assertThat(position.getLockedQuantity()).isEqualTo(5);
        assertThat(position.getAvailableQuantity()).isEqualTo(5);
    }

    @Test
    void shouldThrowExceptionWhenLockingMoreSharesThanAvailable() {
        Position position = Position.builder()
                .quantity(5)
                .lockedQuantity(0)
                .averageBuyPrice(BigDecimal.valueOf(100))
                .totalInvested(BigDecimal.valueOf(500))
                .realizedPnl(BigDecimal.ZERO)
                .build();

        assertThrows(BusinessException.class, () -> position.lockShares(10));
    }

    @Test
    void shouldUnlockSharesCorrectly() {
        Position position = Position.builder()
                .quantity(10)
                .lockedQuantity(5)
                .averageBuyPrice(BigDecimal.valueOf(100))
                .totalInvested(BigDecimal.valueOf(1000))
                .realizedPnl(BigDecimal.ZERO)
                .build();

        position.unlockShares(3);

        assertThat(position.getLockedQuantity()).isEqualTo(2);
    }

    @Test
    void shouldDecreaseQuantityCorrectly() {
        Position position = Position.builder()
                .quantity(10)
                .lockedQuantity(0)
                .averageBuyPrice(BigDecimal.valueOf(100))
                .totalInvested(BigDecimal.valueOf(1000))
                .realizedPnl(BigDecimal.ZERO)
                .build();

        position.decreaseQuantity(4);

        assertThat(position.getQuantity()).isEqualTo(6);
        assertThat(position.getTotalInvested()).isEqualByComparingTo("600");
        assertThat(position.getAverageBuyPrice()).isEqualByComparingTo("100");
    }

    @Test
    void shouldClearValuesWhenDecreasingToZero() {
        Position position = Position.builder()
                .quantity(10)
                .lockedQuantity(0)
                .averageBuyPrice(BigDecimal.valueOf(100))
                .totalInvested(BigDecimal.valueOf(1000))
                .realizedPnl(BigDecimal.ZERO)
                .build();

        position.decreaseQuantity(10);

        assertThat(position.getQuantity()).isEqualTo(0);
        assertThat(position.getTotalInvested()).isEqualByComparingTo("0");
        assertThat(position.getAverageBuyPrice()).isEqualByComparingTo("0");
    }

    @Test
    void shouldThrowExceptionWhenDecreasingMoreThanOwned() {
        Position position = Position.builder()
                .quantity(5)
                .lockedQuantity(0)
                .averageBuyPrice(BigDecimal.valueOf(100))
                .totalInvested(BigDecimal.valueOf(500))
                .realizedPnl(BigDecimal.ZERO)
                .build();

        assertThrows(BusinessException.class, () -> position.decreaseQuantity(10));
    }

    @Test
    void shouldAddRealizedPnlCorrectly() {
        Position position = Position.builder()
                .quantity(10)
                .lockedQuantity(0)
                .averageBuyPrice(BigDecimal.valueOf(100))
                .totalInvested(BigDecimal.valueOf(1000))
                .realizedPnl(BigDecimal.ZERO)
                .build();

        position.addRealizedPnl(BigDecimal.valueOf(50));

        assertThat(position.getRealizedPnl()).isEqualByComparingTo("50");
    }
}