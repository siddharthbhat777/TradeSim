package com.siddharth.tradesim_backend.trading_account.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TradingAccountTest {

    @Test
    void shouldInitializeWithDefaultValues() {
        TradingAccount tradingAccount = TradingAccount.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .baseCurrency("INR")
                .marginLoan(BigDecimal.ZERO)
                .leverage(5)
                .maintenanceMarginPercent(BigDecimal.valueOf(25))
                .build();

        assertThat(tradingAccount.getMarginLoan()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(tradingAccount.getLeverage()).isEqualTo(5);
        assertThat(tradingAccount.getBaseCurrency()).isEqualTo("INR");
    }

    @Test
    void shouldIncreaseMarginLoan() {
        TradingAccount tradingAccount = TradingAccount.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .marginLoan(BigDecimal.valueOf(1000))
                .leverage(5)
                .maintenanceMarginPercent(BigDecimal.valueOf(25))
                .build();

        tradingAccount.increaseMarginLoan(BigDecimal.valueOf(500));

        assertThat(tradingAccount.getMarginLoan()).isEqualByComparingTo(BigDecimal.valueOf(1500));
    }
}