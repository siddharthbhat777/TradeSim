package com.siddharth.tradesim_backend.portfolio.dto;

import com.siddharth.tradesim_backend.order.enums.OrderType;

import java.math.BigDecimal;
import java.util.UUID;

public record TradeExecution(
        UUID buyerId,
        UUID sellerId,
        UUID stockId,
        int quantity,
        BigDecimal executionPrice,
        OrderType buyerOrderType,
        OrderType sellerOrderType,
        BigDecimal buyerLimitPrice
) {
}