package com.siddharth.tradesim_backend.exchange;

import com.siddharth.tradesim_backend.exchange.enums.ExchangeStatus;
import com.siddharth.tradesim_backend.exchange.model.Exchange;
import com.siddharth.tradesim_backend.exchange.model.dto.CreateExchangeRequest;
import com.siddharth.tradesim_backend.exchange.model.dto.ExchangeMarketClockResponse;
import com.siddharth.tradesim_backend.exchange.model.dto.ExchangeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.zone.ZoneRulesException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExchangeService {
    private final ExchangeRepository exchangeRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<ExchangeResponse> fetchExchanges() {
        return exchangeRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ExchangeResponse fetchExchange(UUID exchangeId) {
        Exchange exchange = findExchange(exchangeId);
        return toResponse(exchange);
    }

    @Transactional(readOnly = true)
    public ExchangeMarketClockResponse fetchMarketClock(UUID exchangeId) {
        Exchange exchange = findExchange(exchangeId);
        ZoneId zoneId = parseZoneId(exchange.getTimezone());
        ZonedDateTime exchangeNow = nowAt(zoneId);

        ZonedDateTime marketOpenAt = marketOpenAt(exchange, exchangeNow.toLocalDate(), zoneId);
        ZonedDateTime marketCloseAt = marketCloseAt(exchange, exchangeNow.toLocalDate(), zoneId);

        boolean tradingDay = isTradingDay(exchangeNow.getDayOfWeek());
        boolean marketOpenNow = tradingDay
                && !exchangeNow.isBefore(marketOpenAt)
                && exchangeNow.isBefore(marketCloseAt);

        return new ExchangeMarketClockResponse(
                exchange.getId(),
                exchange.getCode(),
                exchange.getName(),
                exchange.getTimezone(),
                exchangeNow.toLocalDate(),
                exchangeNow.toLocalTime().withNano(0),
                exchangeNow.getDayOfWeek(),
                exchange.getMarketOpenTime(),
                exchange.getMarketCloseTime(),
                tradingDay,
                marketOpenNow,
                exchangeNow.toInstant(),
                marketOpenAt.toInstant(),
                marketCloseAt.toInstant()
        );
    }

    @Transactional
    public ExchangeResponse createExchange(CreateExchangeRequest request) {
        ZoneId zoneId = validateRequest(request);

        if (exchangeRepository.existsByName(request.name())) {
            throw ExchangeException.conflict("Exchange with this name already exists");
        }

        if (exchangeRepository.existsByCode(request.code())) {
            throw ExchangeException.conflict("Exchange with this code already exists");
        }

        try {
            Exchange exchange = Exchange.builder()
                    .name(request.name())
                    .code(request.code())
                    .country(request.country())
                    .timezone(zoneId.getId())
                    .currency(request.currency())
                    .marketOpenTime(request.marketOpenTime())
                    .marketCloseTime(request.marketCloseTime())
                    .status(ExchangeStatus.ACTIVE)
                    .build();

            Exchange saved = exchangeRepository.save(exchange);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw ExchangeException.badRequest("Invalid exchange data");
        }
    }

    @Transactional
    public ExchangeResponse changeStatus(UUID exchangeId, ExchangeStatus status) {
        Exchange exchange = findExchange(exchangeId);

        if (exchange.getStatus() == status) {
            throw ExchangeException.conflict("Exchange already has this status");
        }

        exchange.setStatus(status);
        Exchange saved = exchangeRepository.save(exchange);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public void assertTradingAllowed(UUID exchangeId) {
        Exchange exchange = findExchange(exchangeId);

        if (exchange.getStatus() != ExchangeStatus.ACTIVE) {
            throw ExchangeException.conflict("Exchange is not active");
        }

        ZoneId zoneId = parseZoneId(exchange.getTimezone());
        ZonedDateTime exchangeNow = nowAt(zoneId);

        if (!isTradingDay(exchangeNow.getDayOfWeek())) {
            throw ExchangeException.conflict("Exchange is closed today");
        }

        ZonedDateTime marketOpenAt = marketOpenAt(exchange, exchangeNow.toLocalDate(), zoneId);
        ZonedDateTime marketCloseAt = marketCloseAt(exchange, exchangeNow.toLocalDate(), zoneId);

        if (exchangeNow.isBefore(marketOpenAt) || !exchangeNow.isBefore(marketCloseAt)) {
            throw ExchangeException.conflict("Market is currently closed");
        }
    }

    @Transactional(readOnly = true)
    public Instant resolveDayOrderExpiry(UUID exchangeId) {
        Exchange exchange = findExchange(exchangeId);
        ZoneId zoneId = parseZoneId(exchange.getTimezone());
        ZonedDateTime exchangeNow = nowAt(zoneId);

        return marketCloseAt(exchange, exchangeNow.toLocalDate(), zoneId).toInstant();
    }

    private Exchange findExchange(UUID exchangeId) {
        return exchangeRepository.findById(exchangeId).orElseThrow(() -> ExchangeException.notFound("Exchange not found"));
    }

    private ZoneId validateRequest(CreateExchangeRequest request) {
        ZoneId zoneId = parseZoneId(request.timezone());
        validateMarketHours(request);
        return zoneId;
    }

    private ZoneId parseZoneId(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (ZoneRulesException e) {
            throw ExchangeException.badRequest("Invalid timezone");
        }
    }

    private void validateMarketHours(CreateExchangeRequest request) {
        if (!request.marketOpenTime().isBefore(request.marketCloseTime())) {
            throw ExchangeException.badRequest("Market open time must be before market close time");
        }
    }

    private ZonedDateTime nowAt(ZoneId zoneId) {
        return ZonedDateTime.ofInstant(clock.instant(), zoneId);
    }

    private boolean isTradingDay(DayOfWeek dayOfWeek) {
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    private ZonedDateTime marketOpenAt(Exchange exchange, LocalDate date, ZoneId zoneId) {
        return date.atTime(exchange.getMarketOpenTime()).atZone(zoneId);
    }

    private ZonedDateTime marketCloseAt(Exchange exchange, LocalDate date, ZoneId zoneId) {
        return date.atTime(exchange.getMarketCloseTime()).atZone(zoneId);
    }

    private ExchangeResponse toResponse(Exchange exchange) {
        return new ExchangeResponse(
                exchange.getId(),
                exchange.getName(),
                exchange.getCode(),
                exchange.getCountry(),
                exchange.getTimezone(),
                exchange.getCurrency(),
                exchange.getMarketOpenTime(),
                exchange.getMarketCloseTime(),
                exchange.getStatus()
        );
    }
}