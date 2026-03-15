package com.siddharth.tradesim_backend.portfolio.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PortfolioExposureResponse(
        UUID stockId,
        String symbol,
        BigDecimal positionValue,
        BigDecimal exposurePercent
) {
}