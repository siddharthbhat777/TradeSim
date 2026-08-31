package com.siddharth.tradesim_backend.exchange;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.exchange.enums.ExchangeStatus;
import com.siddharth.tradesim_backend.exchange.model.Exchange;
import com.siddharth.tradesim_backend.exchange.model.dto.CreateExchangeRequest;
import com.siddharth.tradesim_backend.exchange.model.dto.ExchangeMarketClockResponse;
import com.siddharth.tradesim_backend.exchange.model.dto.ExchangeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeServiceTest {

    @Mock
    private ExchangeRepository exchangeRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private ExchangeService exchangeService;

    @Test
    void shouldFetchAllExchanges() {
        Exchange exchange = Exchange.builder()
                .id(UUID.randomUUID())
                .name("TradeSim National Exchange")
                .code("TSX")
                .countryCode("India")
                .timezone("Asia/Kolkata")
                .currency("INR")
                .marketOpenTime(LocalTime.of(9, 15))
                .marketCloseTime(LocalTime.of(15, 30))
                .status(ExchangeStatus.ACTIVE)
                .build();

        when(exchangeRepository.findAll()).thenReturn(List.of(exchange));

        List<ExchangeResponse> responses = exchangeService.fetchExchanges();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().code()).isEqualTo("TSX");
        assertThat(responses.getFirst().currency()).isEqualTo("INR");
    }

    @Test
    void shouldCreateExchangeWhenRequestIsValid() {
        UUID exchangeId = UUID.randomUUID();
        CreateExchangeRequest request = new CreateExchangeRequest(
                "TradeSim National Exchange",
                "TSX",
                "India",
                "Asia/Kolkata",
                "INR",
                LocalTime.of(9, 15),
                LocalTime.of(15, 30)
        );

        when(exchangeRepository.existsByName(request.name())).thenReturn(false);
        when(exchangeRepository.existsByCode(request.code())).thenReturn(false);
        when(exchangeRepository.save(any(Exchange.class))).thenAnswer(invocation -> {
            Exchange exchange = invocation.getArgument(0);
            exchange.setId(exchangeId);
            return exchange;
        });

        ExchangeResponse response = exchangeService.createExchange(request);

        assertThat(response.id()).isEqualTo(exchangeId);
        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.code()).isEqualTo(request.code());
        assertThat(response.status()).isEqualTo(ExchangeStatus.ACTIVE);
        assertThat(response.timezone()).isEqualTo("Asia/Kolkata");
    }

    @Test
    void shouldThrowExceptionWhenTimezoneIsInvalid() {
        CreateExchangeRequest request = new CreateExchangeRequest(
                "TradeSim National Exchange",
                "TSX",
                "India",
                "Asia/InvalidZone",
                "INR",
                LocalTime.of(9, 15),
                LocalTime.of(15, 30)
        );

        BusinessException exception = assertThrows(BusinessException.class, () -> exchangeService.createExchange(request));

        assertThat(exception.getMessage()).isEqualTo("Invalid timezone");
        verify(exchangeRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenMarketOpenTimeIsNotBeforeCloseTime() {
        CreateExchangeRequest request = new CreateExchangeRequest(
                "TradeSim National Exchange",
                "TSX",
                "India",
                "Asia/Kolkata",
                "INR",
                LocalTime.of(15, 30),
                LocalTime.of(9, 15)
        );

        BusinessException exception = assertThrows(BusinessException.class, () -> exchangeService.createExchange(request));

        assertThat(exception.getMessage()).isEqualTo("Market open time must be before market close time");
        verify(exchangeRepository, never()).save(any());
    }

    @Test
    void shouldChangeExchangeStatusWhenValid() {
        UUID exchangeId = UUID.randomUUID();
        Exchange exchange = Exchange.builder()
                .id(exchangeId)
                .name("TradeSim National Exchange")
                .code("TSX")
                .countryCode("India")
                .timezone("Asia/Kolkata")
                .currency("INR")
                .marketOpenTime(LocalTime.of(9, 15))
                .marketCloseTime(LocalTime.of(15, 30))
                .status(ExchangeStatus.ACTIVE)
                .build();

        when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));
        when(exchangeRepository.save(any(Exchange.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExchangeResponse response = exchangeService.changeStatus(exchangeId, ExchangeStatus.HALTED);

        assertThat(response.status()).isEqualTo(ExchangeStatus.HALTED);
    }

    @Test
    void shouldThrowExceptionWhenExchangeAlreadyHasRequestedStatus() {
        UUID exchangeId = UUID.randomUUID();
        Exchange exchange = Exchange.builder()
                .id(exchangeId)
                .status(ExchangeStatus.ACTIVE)
                .build();

        when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));

        BusinessException exception = assertThrows(BusinessException.class, () -> exchangeService.changeStatus(exchangeId, ExchangeStatus.ACTIVE));

        assertThat(exception.getMessage()).isEqualTo("Exchange already has this status");
        verify(exchangeRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenTradingIsAttemptedOnInactiveExchange() {
        UUID exchangeId = UUID.randomUUID();
        Exchange exchange = Exchange.builder()
                .id(exchangeId)
                .status(ExchangeStatus.HALTED)
                .timezone("Asia/Kolkata")
                .marketOpenTime(LocalTime.of(9, 15))
                .marketCloseTime(LocalTime.of(15, 30))
                .build();

        when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));

        BusinessException exception = assertThrows(BusinessException.class, () -> exchangeService.assertTradingAllowed(exchangeId));

        assertThat(exception.getMessage()).isEqualTo("Exchange is not active");
    }

    @Test
    void shouldAllowTradingWhenExchangeLocalTimeIsInsideSession() {
        UUID exchangeId = UUID.randomUUID();
        Exchange exchange = Exchange.builder()
                .id(exchangeId)
                .status(ExchangeStatus.ACTIVE)
                .timezone("Asia/Kolkata")
                .marketOpenTime(LocalTime.of(9, 15))
                .marketCloseTime(LocalTime.of(15, 30))
                .build();

        when(clock.instant()).thenReturn(Instant.parse("2026-04-13T04:00:00Z"));
        when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));

        assertDoesNotThrow(() -> exchangeService.assertTradingAllowed(exchangeId));
    }

    @Test
    void shouldRejectTradingWhenExchangeLocalTimeIsBeforeOpen() {
        UUID exchangeId = UUID.randomUUID();
        Exchange exchange = Exchange.builder()
                .id(exchangeId)
                .status(ExchangeStatus.ACTIVE)
                .timezone("Asia/Kolkata")
                .marketOpenTime(LocalTime.of(9, 15))
                .marketCloseTime(LocalTime.of(15, 30))
                .build();

        when(clock.instant()).thenReturn(Instant.parse("2026-04-13T03:00:00Z"));
        when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));

        BusinessException exception = assertThrows(BusinessException.class, () -> exchangeService.assertTradingAllowed(exchangeId));

        assertThat(exception.getMessage()).isEqualTo("Market is currently closed");
    }

    @Test
    void shouldRejectTradingWhenExchangeLocalTimeIsAfterClose() {
        UUID exchangeId = UUID.randomUUID();
        Exchange exchange = Exchange.builder()
                .id(exchangeId)
                .status(ExchangeStatus.ACTIVE)
                .timezone("Asia/Kolkata")
                .marketOpenTime(LocalTime.of(9, 15))
                .marketCloseTime(LocalTime.of(15, 30))
                .build();

        when(clock.instant()).thenReturn(Instant.parse("2026-04-13T10:30:00Z"));
        when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));

        BusinessException exception = assertThrows(BusinessException.class, () -> exchangeService.assertTradingAllowed(exchangeId));

        assertThat(exception.getMessage()).isEqualTo("Market is currently closed");
    }

    @Test
    void shouldReturnMarketClockUsingExchangeTimezone() {
        UUID exchangeId = UUID.randomUUID();
        Exchange exchange = Exchange.builder()
                .id(exchangeId)
                .name("NYSE Demo")
                .code("NYSE")
                .status(ExchangeStatus.ACTIVE)
                .timezone("America/New_York")
                .currency("USD")
                .marketOpenTime(LocalTime.of(9, 30))
                .marketCloseTime(LocalTime.of(16, 0))
                .build();

        when(clock.instant()).thenReturn(Instant.parse("2026-04-13T14:00:00Z"));
        when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));

        ExchangeMarketClockResponse response = exchangeService.fetchMarketClock(exchangeId);

        assertThat(response.exchangeId()).isEqualTo(exchangeId);
        assertThat(response.timezone()).isEqualTo("America/New_York");
        assertThat(response.localTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(response.tradingDay()).isTrue();
        assertThat(response.marketOpenNow()).isTrue();
        assertThat(response.todayMarketOpenAt()).isEqualTo(Instant.parse("2026-04-13T13:30:00Z"));
        assertThat(response.todayMarketCloseAt()).isEqualTo(Instant.parse("2026-04-13T20:00:00Z"));
    }

    @Test
    void shouldResolveDayOrderExpiryUsingExchangeLocalClose() {
        UUID exchangeId = UUID.randomUUID();
        Exchange exchange = Exchange.builder()
                .id(exchangeId)
                .status(ExchangeStatus.ACTIVE)
                .timezone("Asia/Kolkata")
                .marketOpenTime(LocalTime.of(9, 15))
                .marketCloseTime(LocalTime.of(15, 30))
                .build();

        when(clock.instant()).thenReturn(Instant.parse("2026-04-13T04:00:00Z"));
        when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));

        Instant expiry = exchangeService.resolveDayOrderExpiry(exchangeId);

        assertThat(expiry).isEqualTo(Instant.parse("2026-04-13T10:00:00Z"));
    }
}