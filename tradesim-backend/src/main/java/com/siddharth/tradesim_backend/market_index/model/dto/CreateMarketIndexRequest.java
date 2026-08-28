package com.siddharth.tradesim_backend.market_index.model.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateMarketIndexRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 20) String symbol,
        @NotNull UUID exchangeId,
        @NotNull @DecimalMin("1.0") BigDecimal baseValue
) {
}