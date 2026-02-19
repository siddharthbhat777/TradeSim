package com.siddharth.tradesim_backend.portfolio.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TradeExecution(
        UUID buyerId,
        UUID sellerId,
        UUID stockId,
        int quantity,
        BigDecimal price
) {
}