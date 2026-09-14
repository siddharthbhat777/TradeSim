package com.siddharth.tradesim_backend.forex.model.dto;

import java.math.BigDecimal;

public record FrankfurterRate(
        String date,
        String base,
        String quote,
        BigDecimal rate
) {
}