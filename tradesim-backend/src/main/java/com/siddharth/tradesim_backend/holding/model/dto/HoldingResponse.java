package com.siddharth.tradesim_backend.holding.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record HoldingResponse(
        UUID stockId,
        String stockSymbol,
        int quantity,
        BigDecimal currentPrice
) {
}