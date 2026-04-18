package com.siddharth.tradesim_backend.exchange.model.dto;

import com.siddharth.tradesim_backend.exchange.enums.ExchangeStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeExchangeStatusRequest(
        @NotNull(message = "Exchange status is required")
        ExchangeStatus status
) {
}