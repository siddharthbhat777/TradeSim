package com.siddharth.tradesim_backend.forex.service;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.forex.model.ExchangeRate;
import com.siddharth.tradesim_backend.forex.model.SupportedCurrency;
import com.siddharth.tradesim_backend.forex.model.dto.CountryResponse;
import com.siddharth.tradesim_backend.forex.repository.ExchangeRateRepository;
import com.siddharth.tradesim_backend.forex.repository.SupportedCurrencyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForexServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private SupportedCurrencyRepository supportedCurrencyRepository;

    @InjectMocks
    private ForexService forexService;

    @Test
    void convert_SameCurrency() {
        BigDecimal result = forexService.convert(BigDecimal.valueOf(100), "USD", "USD");
        assertEquals(BigDecimal.valueOf(100), result);
    }

    @Test
    void convert_InrToForeignCurrency() {
        ExchangeRate usdRate = ExchangeRate.builder().rate(new BigDecimal("80.000000")).build();
        when(exchangeRateRepository.findByBaseCurrencyAndQuoteCurrency("INR", "USD")).thenReturn(Optional.of(usdRate));

        BigDecimal result = forexService.convert(BigDecimal.valueOf(100), "INR", "USD");

        assertEquals(new BigDecimal("8000.0000"), result);
    }

    @Test
    void convert_ForeignCurrencyToInr() {
        ExchangeRate usdRate = ExchangeRate.builder().rate(new BigDecimal("80.000000")).build();
        when(exchangeRateRepository.findByBaseCurrencyAndQuoteCurrency("INR", "USD")).thenReturn(Optional.of(usdRate));

        BigDecimal result = forexService.convert(BigDecimal.valueOf(100), "USD", "INR");

        assertEquals(new BigDecimal("1.2500"), result);
    }

    @Test
    void convert_ForeignToForeignCurrency() {
        ExchangeRate usdRate = ExchangeRate.builder().rate(new BigDecimal("80.000000")).build();
        ExchangeRate eurRate = ExchangeRate.builder().rate(new BigDecimal("90.000000")).build();

        when(exchangeRateRepository.findByBaseCurrencyAndQuoteCurrency("INR", "USD")).thenReturn(Optional.of(usdRate));
        when(exchangeRateRepository.findByBaseCurrencyAndQuoteCurrency("INR", "EUR")).thenReturn(Optional.of(eurRate));

        BigDecimal result = forexService.convert(BigDecimal.valueOf(100), "USD", "EUR");

        assertEquals(new BigDecimal("112.5000"), result);
    }

    @Test
    void convert_MissingExchangeRate_ThrowsBusinessException() {
        when(exchangeRateRepository.findByBaseCurrencyAndQuoteCurrency("INR", "XYZ")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                forexService.convert(BigDecimal.valueOf(100), "INR", "XYZ")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("FOREX_RATE_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void fetchActiveSupportedCurrencies_ReturnsActiveAndAlwaysIncludesInr() {
        SupportedCurrency usd = SupportedCurrency.builder().code("USD").isActive(true).build();
        SupportedCurrency eur = SupportedCurrency.builder().code("EUR").isActive(false).build();

        when(supportedCurrencyRepository.findAll()).thenReturn(List.of(usd, eur));

        List<String> result = forexService.fetchActiveSupportedCurrencies();

        assertTrue(result.contains("USD"));
        assertFalse(result.contains("EUR"));
        assertTrue(result.contains("INR"));
    }

    @Test
    void fetchAllCountries_ReturnsValidList() {
        List<CountryResponse> result = forexService.fetchAllCountries();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(c -> c.code().equals("IN") && c.name().equals("India")));
    }

    @Test
    void resolveNativeCurrencyFromCountryCode_ValidCode_ReturnsCurrency() {
        String resultUpper = forexService.resolveNativeCurrencyFromCountryCode("IN");
        assertEquals("INR", resultUpper);

        String resultLower = forexService.resolveNativeCurrencyFromCountryCode("us");
        assertEquals("USD", resultLower);
    }

    @Test
    void resolveNativeCurrencyFromCountryCode_InvalidCode_ReturnsNull() {
        assertNull(forexService.resolveNativeCurrencyFromCountryCode(null));
        assertNull(forexService.resolveNativeCurrencyFromCountryCode(""));
        assertNull(forexService.resolveNativeCurrencyFromCountryCode("INVALID"));
    }
}