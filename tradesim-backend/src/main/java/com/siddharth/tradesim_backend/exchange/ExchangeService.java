package com.siddharth.tradesim_backend.exchange;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.exchange.enums.ExchangeStatus;
import com.siddharth.tradesim_backend.exchange.model.Exchange;
import com.siddharth.tradesim_backend.exchange.model.dto.CreateExchangeRequest;
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

    @Transactional(readOnly = true)
    public List<ExchangeResponse> fetchExchanges() {
        return exchangeRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ExchangeResponse fetchExchange(UUID exchangeId) {
        Exchange exchange = exchangeRepository.findById(exchangeId).orElseThrow(() -> new BusinessException("Exchange not found"));
        return toResponse(exchange);
    }

    @Transactional
    public ExchangeResponse createExchange(CreateExchangeRequest request) {
        ZoneId zoneId = validateRequest(request);

        if (exchangeRepository.existsByName(request.name())) {
            throw new BusinessException("Exchange with this name already exists");
        }

        if (exchangeRepository.existsByCode(request.code())) {
            throw new BusinessException("Exchange with this code already exists");
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
            throw new BusinessException("Invalid exchange data");
        }
    }

    @Transactional
    public ExchangeResponse changeStatus(UUID exchangeId, ExchangeStatus status) {
        Exchange exchange = exchangeRepository.findById(exchangeId).orElseThrow(() -> new BusinessException("Exchange not found"));

        if (exchange.getStatus() == status) {
            throw new BusinessException("Exchange already has this status");
        }

        exchange.setStatus(status);
        Exchange saved = exchangeRepository.save(exchange);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public void assertTradingAllowed(UUID exchangeId) {
        Exchange exchange = exchangeRepository.findById(exchangeId).orElseThrow(() -> new BusinessException("Exchange not found"));

        if (exchange.getStatus() != ExchangeStatus.ACTIVE) {
            throw new BusinessException("Exchange is not active");
        }

        ZonedDateTime exchangeNow = ZonedDateTime.now(parseZoneId(exchange.getTimezone()));
        DayOfWeek day = exchangeNow.getDayOfWeek();

        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            throw new BusinessException("Exchange is closed today");
        }

        LocalTime currentTime = exchangeNow.toLocalTime();

        if (currentTime.isBefore(exchange.getMarketOpenTime()) || currentTime.isAfter(exchange.getMarketCloseTime())) {
            throw new BusinessException("Market is currently closed");
        }
    }

    @Transactional(readOnly = true)
    public Instant resolveDayOrderExpiry(UUID exchangeId) {
        Exchange exchange = exchangeRepository.findById(exchangeId).orElseThrow(() -> new BusinessException("Exchange not found"));

        ZoneId zoneId = parseZoneId(exchange.getTimezone());
        ZonedDateTime exchangeNow = ZonedDateTime.now(zoneId);

        return exchangeNow.toLocalDate()
                .atTime(exchange.getMarketCloseTime())
                .atZone(zoneId)
                .toInstant();
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
            throw new BusinessException("Invalid timezone");
        }
    }

    private void validateMarketHours(CreateExchangeRequest request) {
        if (!request.marketOpenTime().isBefore(request.marketCloseTime())) {
            throw new BusinessException("Market open time must be before market close time");
        }
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