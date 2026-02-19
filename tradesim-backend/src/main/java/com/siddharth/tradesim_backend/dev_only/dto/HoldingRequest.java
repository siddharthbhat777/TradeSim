package com.siddharth.tradesim_backend.dev_only.dto;

import java.util.UUID;

public record HoldingRequest(
        UUID stockId,
        int quantity
) {
}