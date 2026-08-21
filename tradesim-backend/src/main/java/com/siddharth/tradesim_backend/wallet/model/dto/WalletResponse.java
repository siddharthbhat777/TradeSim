package com.siddharth.tradesim_backend.wallet.model.dto;

import java.util.List;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        UUID userId,
        List<WalletBucketResponse> buckets
) {
}