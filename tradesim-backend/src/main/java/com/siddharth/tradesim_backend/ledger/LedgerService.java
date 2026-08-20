package com.siddharth.tradesim_backend.ledger;

import com.siddharth.tradesim_backend.ledger.enums.LedgerEntryType;
import com.siddharth.tradesim_backend.ledger.model.LedgerEntry;
import com.siddharth.tradesim_backend.ledger.model.dto.LedgerEntryResponse;
import com.siddharth.tradesim_backend.wallet.model.WalletBucket;
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
    public void recordBuyLimitMarginLock(WalletBucket bucket, TradingAccount tradingAccount, BigDecimal amount, UUID stockId, UUID orderId) {
        saveEntry(bucket, tradingAccount, stockId, orderId, null, LedgerEntryType.BUY_LIMIT_MARGIN_LOCK, amount, "Locked margin for BUY LIMIT order");
    }

    @Transactional
    public void recordBuyLimitMarginUnlock(WalletBucket bucket, TradingAccount tradingAccount, BigDecimal amount, UUID stockId, UUID orderId) {
        saveEntry(bucket, tradingAccount, stockId, orderId, null, LedgerEntryType.BUY_LIMIT_MARGIN_UNLOCK, amount, "Unlocked reserved margin for BUY LIMIT order");
    }

    @Transactional
    public void recordBuyOrderMarginLock(WalletBucket bucket, TradingAccount tradingAccount, BigDecimal amount, UUID stockId, UUID orderId) {
        saveEntry(bucket, tradingAccount, stockId, orderId, null, LedgerEntryType.BUY_ORDER_MARGIN_LOCK, amount, "Locked margin for BUY order");
    }

    @Transactional
    public void recordBuyOrderMarginUnlock(WalletBucket bucket, TradingAccount tradingAccount, BigDecimal amount, UUID stockId, UUID orderId) {
        saveEntry(bucket, tradingAccount, stockId, orderId, null, LedgerEntryType.BUY_ORDER_MARGIN_UNLOCK, amount, "Unlocked reserved margin for BUY order");
    }

    @Transactional
    public void recordTradeMarginDebit(WalletBucket bucket, TradingAccount tradingAccount, BigDecimal amount, UUID stockId, UUID orderId) {
        saveEntry(bucket, tradingAccount, stockId, orderId, null, LedgerEntryType.TRADE_MARGIN_DEBIT, amount, "Debited cash margin during trade settlement");
    }

    @Transactional
    public void recordTradeProceedsCredit(WalletBucket bucket, TradingAccount tradingAccount, BigDecimal amount, UUID stockId, UUID orderId) {
        saveEntry(bucket, tradingAccount, stockId, orderId, null, LedgerEntryType.TRADE_PROCEEDS_CREDIT, amount, "Credited trade proceeds after settlement");
    }

    @Transactional
    public void recordMarginLoanIncrease(WalletBucket bucket, TradingAccount tradingAccount, BigDecimal amount, UUID stockId, UUID orderId) {
        saveEntry(bucket, tradingAccount, stockId, orderId, null, LedgerEntryType.MARGIN_LOAN_INCREASE, amount, "Increased margin loan during leveraged buy settlement");
    }

    @Transactional
    public void recordMarginLoanRepayment(WalletBucket bucket, TradingAccount tradingAccount, BigDecimal amount, UUID stockId, UUID orderId) {
        saveEntry(bucket, tradingAccount, stockId, orderId, null, LedgerEntryType.MARGIN_LOAN_REPAYMENT, amount, "Repaid margin loan from sell proceeds");
    }

    @Transactional
    public void recordIpoSubscriptionLock(WalletBucket bucket, TradingAccount tradingAccount, BigDecimal amount, UUID stockId, UUID ipoOfferId) {
        saveEntry(bucket, tradingAccount, stockId, null, ipoOfferId, LedgerEntryType.IPO_SUBSCRIPTION_LOCK, amount, "Locked funds for IPO subscription");
    }

    @Transactional
    public void recordIpoSubscriptionUnlock(WalletBucket bucket, TradingAccount tradingAccount, BigDecimal amount, UUID stockId, UUID ipoOfferId) {
        saveEntry(bucket, tradingAccount, stockId, null, ipoOfferId, LedgerEntryType.IPO_SUBSCRIPTION_UNLOCK, amount, "Unlocked funds for non-allotted IPO subscription");
    }

    @Transactional
    public void recordIpoAllotmentDebit(WalletBucket bucket, TradingAccount tradingAccount, BigDecimal amount, UUID stockId, UUID ipoOfferId) {
        saveEntry(bucket, tradingAccount, stockId, null, ipoOfferId, LedgerEntryType.IPO_ALLOTMENT_DEBIT, amount, "Debited locked funds for IPO allotment");
    }

    @Transactional
    public void recordFxConversionFee(WalletBucket bucket, TradingAccount tradingAccount, BigDecimal amount, UUID stockId, UUID orderId, UUID ipoOfferId, String sourceCurrency, String targetCurrency) {
        String description = String.format("FX Conversion Fee (%s to %s)", sourceCurrency, targetCurrency);
        saveEntry(bucket, tradingAccount, stockId, orderId, ipoOfferId, LedgerEntryType.FX_CONVERSION_FEE, amount, description);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> fetchMyLedger(UUID userId) {
        return ledgerEntryRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    private void saveEntry(WalletBucket bucket, TradingAccount tradingAccount, UUID stockId, UUID orderId, UUID ipoOfferId, LedgerEntryType type, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Ledger amount must be non-negative and not null");
        }

        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        LedgerEntry ledgerEntry = LedgerEntry.builder()
                .tradingAccountId(tradingAccount != null ? tradingAccount.getId() : null)
                .userId(bucket.getWallet().getUserId())
                .stockId(stockId)
                .orderId(orderId)
                .ipoOfferId(ipoOfferId)
                .type(type)
                .amount(amount)
                .currency(bucket.getCurrency())
                .balanceAfter(bucket.getBalance())
                .lockedBalanceAfter(bucket.getLockedBalance())
                .marginLoanAfter(tradingAccount != null ? tradingAccount.getMarginLoan() : BigDecimal.ZERO)
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
                ledgerEntry.getIpoOfferId(),
                ledgerEntry.getType(),
                ledgerEntry.getAmount(),
                ledgerEntry.getCurrency(),
                ledgerEntry.getBalanceAfter(),
                ledgerEntry.getLockedBalanceAfter(),
                ledgerEntry.getMarginLoanAfter(),
                ledgerEntry.getDescription(),
                ledgerEntry.getCreatedAt()
        );
    }
}