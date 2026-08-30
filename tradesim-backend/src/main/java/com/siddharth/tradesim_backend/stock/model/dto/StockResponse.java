package com.siddharth.tradesim_backend.stock.model.dto;

import com.siddharth.tradesim_backend.stock.enums.MarketCapCategory;
import com.siddharth.tradesim_backend.stock.enums.Sector;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record StockResponse(
        UUID id,
        String symbol,
        String companyName,
        BigDecimal currentPrice,
        Sector sector,
        StockStatus status,
        Long dayVolume,
        BigDecimal marketCap,
        MarketCapCategory marketCapCategory
) {
}