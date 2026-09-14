package com.siddharth.tradesim_backend.market_index.model.dto;

import java.util.UUID;

public record MarketIndexConstituentResponse(
        UUID stockId,
        String symbol,
        String companyName
) {
}