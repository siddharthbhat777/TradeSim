package com.siddharth.tradesim_backend.trading_account.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TradingAccountResponse(
        UUID id,
        UUID userId,
        String baseCurrency,
        BigDecimal marginLoan,
        int leverage,
        BigDecimal maintenanceMarginPercent,
        Instant createdAt,
        Instant updatedAt
) {
}