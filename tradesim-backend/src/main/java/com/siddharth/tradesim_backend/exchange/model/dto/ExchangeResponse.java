package com.siddharth.tradesim_backend.exchange.model.dto;

import com.siddharth.tradesim_backend.exchange.enums.ExchangeStatus;

import java.time.LocalTime;
import java.util.UUID;

public record ExchangeResponse(
        UUID id,
        String name,
        String code,
        String country,
        String timezone,
        String currency,
        LocalTime marketOpenTime,
        LocalTime marketCloseTime,
        ExchangeStatus status
) {
}