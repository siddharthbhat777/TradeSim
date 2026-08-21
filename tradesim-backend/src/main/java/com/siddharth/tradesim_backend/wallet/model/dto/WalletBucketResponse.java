package com.siddharth.tradesim_backend.wallet.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletBucketResponse(
        UUID id,
        String currency,
        BigDecimal balance,
        BigDecimal lockedBalance,
        BigDecimal availableBalance
) {
}