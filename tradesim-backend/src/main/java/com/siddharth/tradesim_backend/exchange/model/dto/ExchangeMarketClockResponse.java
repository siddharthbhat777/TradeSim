package com.siddharth.tradesim_backend.exchange.model.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ExchangeMarketClockResponse(
        UUID exchangeId,
        String exchangeCode,
        String exchangeName,
        String timezone,
        LocalDate localDate,
        LocalTime localTime,
        DayOfWeek localDayOfWeek,
        LocalTime marketOpenTime,
        LocalTime marketCloseTime,
        boolean tradingDay,
        boolean marketOpenNow,
        Instant currentInstant,
        Instant todayMarketOpenAt,
        Instant todayMarketCloseAt
) {
}