package com.siddharth.tradesim_backend.ledger;

import com.siddharth.tradesim_backend.ledger.enums.LedgerEntryType;
import com.siddharth.tradesim_backend.ledger.model.LedgerEntry;
import com.siddharth.tradesim_backend.ledger.model.dto.LedgerEntryResponse;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import com.siddharth.tradesim_backend.wallet.model.Wallet;
import com.siddharth.tradesim_backend.wallet.model.WalletBucket;
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
    void shouldRecordIpoSubscriptionLockEntry() {
        UUID ipoOfferId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Wallet wallet = Wallet.builder().userId(userId).build();
        WalletBucket bucket = WalletBucket.builder()
                .wallet(wallet)
                .currency("USD")
                .balance(BigDecimal.valueOf(100000))
                .lockedBalance(BigDecimal.valueOf(5000))
                .build();

        TradingAccount tradingAccount = TradingAccount.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .marginLoan(BigDecimal.ZERO)
                .build();

        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ledgerService.recordIpoSubscriptionLock(bucket, tradingAccount, BigDecimal.valueOf(5000), stockId, ipoOfferId);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(captor.capture());

        LedgerEntry ledgerEntry = captor.getValue();
        assertThat(ledgerEntry.getType()).isEqualTo(LedgerEntryType.IPO_SUBSCRIPTION_LOCK);
        assertThat(ledgerEntry.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(ledgerEntry.getStockId()).isEqualTo(stockId);
        assertThat(ledgerEntry.getIpoOfferId()).isEqualTo(ipoOfferId);
        assertThat(ledgerEntry.getLockedBalanceAfter()).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

    @Test
    void shouldFetchLedgerEntriesForUser() {
        UUID userId = UUID.randomUUID();
        UUID tradingAccountId = UUID.randomUUID();
        UUID ipoOfferId = UUID.randomUUID();

        LedgerEntry ledgerEntry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .tradingAccountId(tradingAccountId)
                .userId(userId)
                .ipoOfferId(ipoOfferId)
                .type(LedgerEntryType.IPO_SUBSCRIPTION_LOCK)
                .amount(BigDecimal.valueOf(5000))
                .balanceAfter(BigDecimal.valueOf(100000))
                .lockedBalanceAfter(BigDecimal.valueOf(5000))
                .marginLoanAfter(BigDecimal.ZERO)
                .description("Locked funds for IPO subscription")
                .build();
        ledgerEntry.setCreatedAt(Instant.now());
        ledgerEntry.setUpdatedAt(Instant.now());

        when(ledgerEntryRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(ledgerEntry));

        List<LedgerEntryResponse> responses = ledgerService.fetchMyLedger(userId);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().type()).isEqualTo(LedgerEntryType.IPO_SUBSCRIPTION_LOCK);
        assertThat(responses.getFirst().amount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(responses.getFirst().ipoOfferId()).isEqualTo(ipoOfferId);
    }
}