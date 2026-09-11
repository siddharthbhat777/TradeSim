package com.siddharth.tradesim_backend.auth.model.dto;

import java.math.BigDecimal;

public record BankBalanceResponse(
        BigDecimal bankBalance
) {
}