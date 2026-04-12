package com.siddharth.tradesim_backend.order.model.dto;

import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.enums.TimeInForce;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

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
        BigDecimal limitPrice
) {
}