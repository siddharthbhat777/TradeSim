package com.siddharth.tradesim_backend.trading_account;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import com.siddharth.tradesim_backend.trading_account.model.dto.TradingAccountResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradingAccountServiceTest {

    @Mock
    private TradingAccountRepository tradingAccountRepository;

    @InjectMocks
    private TradingAccountService tradingAccountService;

    @Test
    void shouldCreateDefaultTradingAccountForUser() {
        UUID userId = UUID.randomUUID();

        when(tradingAccountRepository.existsByUserId(userId)).thenReturn(false);
        when(tradingAccountRepository.save(any(TradingAccount.class))).thenAnswer(invocation -> {
            TradingAccount tradingAccount = invocation.getArgument(0);
            tradingAccount.setId(UUID.randomUUID());
            return tradingAccount;
        });

        TradingAccount tradingAccount = tradingAccountService.createTradingAccountForUser(userId);

        assertThat(tradingAccount.getUserId()).isEqualTo(userId);
        assertThat(tradingAccount.getBaseCurrency()).isEqualTo("INR");
        assertThat(tradingAccount.getMarginLoan()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(tradingAccount.getLeverage()).isEqualTo(5);
        assertThat(tradingAccount.getMaintenanceMarginPercent()).isEqualByComparingTo(BigDecimal.valueOf(25));
    }

    @Test
    void shouldFetchTradingAccountResponseForUser() {
        UUID userId = UUID.randomUUID();

        TradingAccount tradingAccount = TradingAccount.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .baseCurrency("INR")
                .marginLoan(BigDecimal.valueOf(25000))
                .leverage(5)
                .maintenanceMarginPercent(BigDecimal.valueOf(25))
                .build();

        when(tradingAccountRepository.findByUserId(userId)).thenReturn(Optional.of(tradingAccount));

        TradingAccountResponse response = tradingAccountService.fetchMyTradingAccount(userId);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.baseCurrency()).isEqualTo("INR");
        assertThat(response.marginLoan()).isEqualByComparingTo(BigDecimal.valueOf(25000));
        assertThat(response.leverage()).isEqualTo(5);
    }

    @Test
    void shouldThrowWhenTradingAccountIsMissing() {
        UUID userId = UUID.randomUUID();

        when(tradingAccountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> tradingAccountService.fetchMyTradingAccount(userId));
    }
}