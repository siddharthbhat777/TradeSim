package com.siddharth.tradesim_backend.position;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.position.model.Position;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PositionTest {

    @Test
    void shouldCalculateAverageBuyPriceCorrectly() {
        Position position = Position.builder()
                .quantity(10)
                .lockedQuantity(0)
                .averageBuyPrice(BigDecimal.valueOf(100))
                .realizedPnl(BigDecimal.ZERO)
                .build();

        position.updateAverageBuyPrice(BigDecimal.valueOf(200), 10);

        assertThat(position.getAverageBuyPrice()).isEqualByComparingTo("150.0000");
    }

    @Test
    void shouldLockSharesCorrectly() {
        Position position = Position.builder()
                .quantity(10)
                .lockedQuantity(0)
                .averageBuyPrice(BigDecimal.valueOf(100))
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
                .realizedPnl(BigDecimal.ZERO)
                .build();

        position.decreaseQuantity(4);

        assertThat(position.getQuantity()).isEqualTo(6);
    }

    @Test
    void shouldThrowExceptionWhenDecreasingMoreThanOwned() {
        Position position = Position.builder()
                .quantity(5)
                .lockedQuantity(0)
                .averageBuyPrice(BigDecimal.valueOf(100))
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
                .realizedPnl(BigDecimal.ZERO)
                .build();

        position.addRealizedPnl(BigDecimal.valueOf(50));

        assertThat(position.getRealizedPnl()).isEqualByComparingTo("50");
    }
}