package com.siddharth.tradesim_backend.user.dto;

import java.math.BigDecimal;

public record BankBalanceResponse(
        BigDecimal bankBalance
) {
}