package com.siddharth.tradesim_backend.order.orderbook;

import java.math.BigDecimal;

public record MatchResult(
        boolean priceBandHit,
        boolean executedSomething,
        BigDecimal lastExecutionPrice
) {
}