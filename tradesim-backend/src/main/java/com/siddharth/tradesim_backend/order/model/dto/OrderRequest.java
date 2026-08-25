package com.siddharth.tradesim_backend.order.model.dto;

import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.enums.TimeInForce;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderRequest(
        @NotNull
        UUID stockId,

        @Min(1)
        int quantity,

        @NotNull
        OrderSide side,

        @NotNull
        OrderType orderType,

        @NotNull
        TimeInForce timeInForce,

        @DecimalMin("0.01")
        BigDecimal limitPrice,

        @Pattern(regexp = "^[A-Z]{3}$", message = "Funding currency must be a valid 3-letter ISO code")
        String fundingCurrency
) {
}