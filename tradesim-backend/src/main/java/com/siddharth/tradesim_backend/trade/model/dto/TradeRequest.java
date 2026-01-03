package com.siddharth.tradesim_backend.trade.model.dto;

import com.siddharth.tradesim_backend.trade.enums.OrderType;
import com.siddharth.tradesim_backend.trade.enums.Type;
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
    private Type type;

    @NotNull
    private OrderType orderType;

    @DecimalMin("0.01")
    private BigDecimal limitPrice;
}