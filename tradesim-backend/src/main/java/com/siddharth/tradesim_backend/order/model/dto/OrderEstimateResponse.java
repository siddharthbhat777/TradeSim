package com.siddharth.tradesim_backend.order.model.dto;

import java.math.BigDecimal;

public record OrderEstimateResponse(
        BigDecimal subtotalInFundingCurrency,
        BigDecimal safetyBufferInFundingCurrency,
        BigDecimal fxFee,
        BigDecimal finalTotal,
        boolean hasFunds,
        String fundingCurrency
) {
}