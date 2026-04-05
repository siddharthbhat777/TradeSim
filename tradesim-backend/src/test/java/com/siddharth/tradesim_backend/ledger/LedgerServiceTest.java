package com.siddharth.tradesim_backend.ledger;

import com.siddharth.tradesim_backend.ledger.enums.LedgerEntryType;
import com.siddharth.tradesim_backend.ledger.model.LedgerEntry;
import com.siddharth.tradesim_backend.ledger.model.dto.LedgerEntryResponse;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private LedgerService ledgerService;

    @Test
    void shouldRecordInitialCreditEntry() {
        TradingAccount tradingAccount = TradingAccount.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .balance(BigDecimal.valueOf(10000000))
                .lockedBalance(BigDecimal.ZERO)
                .marginLoan(BigDecimal.ZERO)
                .build();

        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ledgerService.recordInitialCredit(tradingAccount, BigDecimal.valueOf(10000000));

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(captor.capture());

        LedgerEntry ledgerEntry = captor.getValue();
        assertThat(ledgerEntry.getType()).isEqualTo(LedgerEntryType.INITIAL_CREDIT);
        assertThat(ledgerEntry.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000000));
        assertThat(ledgerEntry.getBalanceAfter()).isEqualByComparingTo(BigDecimal.valueOf(10000000));
        assertThat(ledgerEntry.getLockedBalanceAfter()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ledgerEntry.getMarginLoanAfter()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldFetchLedgerEntriesForUser() {
        UUID userId = UUID.randomUUID();
        UUID tradingAccountId = UUID.randomUUID();

        LedgerEntry ledgerEntry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .tradingAccountId(tradingAccountId)
                .userId(userId)
                .type(LedgerEntryType.BUY_LIMIT_MARGIN_LOCK)
                .amount(BigDecimal.valueOf(4800))
                .balanceAfter(BigDecimal.valueOf(10000000))
                .lockedBalanceAfter(BigDecimal.valueOf(4800))
                .marginLoanAfter(BigDecimal.ZERO)
                .description("Locked margin for BUY LIMIT order")
                .build();
        ledgerEntry.setCreatedAt(Instant.now());
        ledgerEntry.setUpdatedAt(Instant.now());

        when(ledgerEntryRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(ledgerEntry));

        List<LedgerEntryResponse> responses = ledgerService.fetchMyLedger(userId);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().type()).isEqualTo(LedgerEntryType.BUY_LIMIT_MARGIN_LOCK);
        assertThat(responses.getFirst().amount()).isEqualByComparingTo(BigDecimal.valueOf(4800));
    }
}