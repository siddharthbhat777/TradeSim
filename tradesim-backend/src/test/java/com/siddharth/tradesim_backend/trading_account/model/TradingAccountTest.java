package com.siddharth.tradesim_backend.trading_account.model;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TradingAccountTest {

    @Test
    void shouldDebitLockedFundsWhenEnoughLockedBalanceExists() {
        TradingAccount tradingAccount = TradingAccount.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .balance(BigDecimal.valueOf(10000))
                .lockedBalance(BigDecimal.valueOf(5000))
                .marginLoan(BigDecimal.ZERO)
                .leverage(5)
                .maintenanceMarginPercent(BigDecimal.valueOf(25))
                .build();

        tradingAccount.debitLockedFunds(BigDecimal.valueOf(5000));

        assertThat(tradingAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(tradingAccount.getLockedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(tradingAccount.getAvailableBalance()).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

    @Test
    void shouldThrowWhenLockedDebitExceedsLockedBalance() {
        TradingAccount tradingAccount = TradingAccount.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .balance(BigDecimal.valueOf(10000))
                .lockedBalance(BigDecimal.valueOf(3000))
                .marginLoan(BigDecimal.ZERO)
                .leverage(5)
                .maintenanceMarginPercent(BigDecimal.valueOf(25))
                .build();

        BusinessException exception = assertThrows(BusinessException.class, () -> tradingAccount.debitLockedFunds(BigDecimal.valueOf(5000)));

        assertThat(exception.getMessage()).isEqualTo("Insufficient locked balance");
    }
}