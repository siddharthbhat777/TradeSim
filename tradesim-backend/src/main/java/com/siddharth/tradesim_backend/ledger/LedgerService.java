package com.siddharth.tradesim_backend.ledger;

import com.siddharth.tradesim_backend.ledger.enums.LedgerEntryType;
import com.siddharth.tradesim_backend.ledger.model.LedgerEntry;
import com.siddharth.tradesim_backend.ledger.model.dto.LedgerEntryResponse;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerService {
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional
    public void recordInitialCredit(TradingAccount tradingAccount, BigDecimal amount) {
        saveEntry(
                tradingAccount,
                null,
                null,
                LedgerEntryType.INITIAL_CREDIT,
                amount,
                "Initial trading account funding"
        );
    }

    @Transactional
    public void recordBuyLimitMarginLock(TradingAccount tradingAccount, BigDecimal amount, UUID stockId, UUID orderId) {
        saveEntry(
                tradingAccount,
                stockId,
                orderId,
                LedgerEntryType.BUY_LIMIT_MARGIN_LOCK,
                amount,
                "Locked margin for BUY LIMIT order"
        );
    }

    @Transactional
    public void recordBuyLimitMarginUnlock(TradingAccount tradingAccount, BigDecimal amount, UUID stockId, UUID orderId) {
        saveEntry(
                tradingAccount,
                stockId,
                orderId,
                LedgerEntryType.BUY_LIMIT_MARGIN_UNLOCK,
                amount,
                "Unlocked reserved margin for BUY LIMIT order"
        );
    }

    @Transactional
    public void recordTradeMarginDebit(TradingAccount tradingAccount, BigDecimal amount, UUID stockId, UUID orderId) {
        saveEntry(
                tradingAccount,
                stockId,
                orderId,
                LedgerEntryType.TRADE_MARGIN_DEBIT,
                amount,
                "Debited cash margin during trade settlement"
        );
    }

    @Transactional
    public void recordTradeProceedsCredit(TradingAccount tradingAccount, BigDecimal amount, UUID stockId, UUID orderId) {
        saveEntry(
                tradingAccount,
                stockId,
                orderId,
                LedgerEntryType.TRADE_PROCEEDS_CREDIT,
                amount,
                "Credited trade proceeds after settlement"
        );
    }

    @Transactional
    public void recordMarginLoanIncrease(TradingAccount tradingAccount, BigDecimal amount, UUID stockId, UUID orderId) {
        saveEntry(
                tradingAccount,
                stockId,
                orderId,
                LedgerEntryType.MARGIN_LOAN_INCREASE,
                amount,
                "Increased margin loan during leveraged buy settlement"
        );
    }

    @Transactional
    public void recordMarginLoanRepayment(TradingAccount tradingAccount, BigDecimal amount, UUID stockId, UUID orderId) {
        saveEntry(
                tradingAccount,
                stockId,
                orderId,
                LedgerEntryType.MARGIN_LOAN_REPAYMENT,
                amount,
                "Repaid margin loan from sell proceeds"
        );
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> fetchMyLedger(UUID userId) {
        return ledgerEntryRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    private void saveEntry(
            TradingAccount tradingAccount,
            UUID stockId,
            UUID orderId,
            LedgerEntryType type,
            BigDecimal amount,
            String description
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ledger amount must be positive");
        }

        LedgerEntry ledgerEntry = LedgerEntry.builder()
                .tradingAccountId(tradingAccount.getId())
                .userId(tradingAccount.getUserId())
                .stockId(stockId)
                .orderId(orderId)
                .type(type)
                .amount(amount)
                .balanceAfter(tradingAccount.getBalance())
                .lockedBalanceAfter(tradingAccount.getLockedBalance())
                .marginLoanAfter(tradingAccount.getMarginLoan())
                .description(description)
                .build();

        ledgerEntryRepository.save(ledgerEntry);
    }

    private LedgerEntryResponse toResponse(LedgerEntry ledgerEntry) {
        return new LedgerEntryResponse(
                ledgerEntry.getId(),
                ledgerEntry.getTradingAccountId(),
                ledgerEntry.getUserId(),
                ledgerEntry.getStockId(),
                ledgerEntry.getOrderId(),
                ledgerEntry.getType(),
                ledgerEntry.getAmount(),
                ledgerEntry.getBalanceAfter(),
                ledgerEntry.getLockedBalanceAfter(),
                ledgerEntry.getMarginLoanAfter(),
                ledgerEntry.getDescription(),
                ledgerEntry.getCreatedAt()
        );
    }
}