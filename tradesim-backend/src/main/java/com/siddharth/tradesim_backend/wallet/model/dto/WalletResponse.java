package com.siddharth.tradesim_backend.wallet.model.dto;

import com.siddharth.tradesim_backend.wallet.enums.MultiCurrencyStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        UUID userId,
        MultiCurrencyStatus multiCurrencyStatus,
        String rejectionReason,
        List<WalletBucketResponse> buckets,
        Instant createdAt
) {
}