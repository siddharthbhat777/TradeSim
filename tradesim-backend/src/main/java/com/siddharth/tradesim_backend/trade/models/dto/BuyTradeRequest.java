package com.siddharth.tradesim_backend.trade.models.dto;

import com.siddharth.tradesim_backend.trade.enums.OrderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class BuyTradeRequest {
        @NotNull
        private UUID stockId;

        @Min(1)
        private int quantity;

        @NotNull
        private OrderType orderType;

        @DecimalMin("0.01")
        private BigDecimal limitPrice;
}