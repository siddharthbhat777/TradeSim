package com.siddharth.tradesim_backend.dev_only.dto;

import java.util.UUID;

public record PositionRequest(
        UUID stockId,
        int quantity
) {
}