package com.siddharth.tradesim_backend.market_index.model.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddConstituentRequest(
        @NotNull UUID stockId
) {
}