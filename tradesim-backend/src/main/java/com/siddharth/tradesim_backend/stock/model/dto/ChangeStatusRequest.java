package com.siddharth.tradesim_backend.stock.model.dto;

import com.siddharth.tradesim_backend.stock.enums.StockStatus;

public record ChangeStatusRequest(
        StockStatus status
) {
}