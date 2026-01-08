package com.siddharth.tradesim_backend.order.model.dto;

import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class TradeRequest {
    @NotNull
    private UUID stockId;

    @Min(1)
    private int quantity;

    @NotNull
    private OrderSide type;

    @NotNull
    private OrderType orderType;

    @DecimalMin("0.01")
    private BigDecimal limitPrice;
}