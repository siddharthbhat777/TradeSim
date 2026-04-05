package com.siddharth.tradesim_backend.ledger.model.dto;

import com.siddharth.tradesim_backend.ledger.enums.LedgerEntryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID id,
        UUID tradingAccountId,
        UUID userId,
        UUID stockId,
        UUID orderId,
        LedgerEntryType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        BigDecimal lockedBalanceAfter,
        BigDecimal marginLoanAfter,
        String description,
        Instant createdAt
) {
}