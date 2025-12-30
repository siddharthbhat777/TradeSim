package com.siddharth.tradesim_backend.stock.models.dto;

import com.siddharth.tradesim_backend.stock.enums.Sector;

import java.math.BigDecimal;
import java.util.UUID;

public record StockResponse(
        UUID id,
        String symbol,
        String companyName,
        BigDecimal currentPrice,
        Sector sector,
        boolean active
) {
}