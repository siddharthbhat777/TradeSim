package com.siddharth.tradesim_backend.trading_account.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TradingAccountResponse(
        UUID id,
        UUID userId,
        BigDecimal balance,
        BigDecimal lockedBalance,
        BigDecimal availableBalance,
        BigDecimal marginLoan,
        int leverage,
        BigDecimal maintenanceMarginPercent,
        Instant createdAt,
        Instant updatedAt
) {
}