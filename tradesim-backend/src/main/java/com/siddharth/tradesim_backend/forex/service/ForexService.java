package com.siddharth.tradesim_backend.forex.service;

import com.ibm.icu.util.TimeZone;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.forex.model.ExchangeRate;
import com.siddharth.tradesim_backend.forex.model.SupportedCurrency;
import com.siddharth.tradesim_backend.forex.model.dto.CountryDefaultsResponse;
import com.siddharth.tradesim_backend.forex.model.dto.CountryResponse;
import com.siddharth.tradesim_backend.forex.repository.ExchangeRateRepository;
import com.siddharth.tradesim_backend.forex.repository.SupportedCurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ForexService {
    private final ExchangeRateRepository exchangeRateRepository;
    private final SupportedCurrencyRepository supportedCurrencyRepository;

    @Transactional(readOnly = true)
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }

        BigDecimal sourceFromInrRate = getInrRate(fromCurrency);
        BigDecimal targetFromInrRate = getInrRate(toCurrency);

        BigDecimal amountInInr = amount.divide(sourceFromInrRate, 8, RoundingMode.HALF_UP);
        return amountInInr.multiply(targetFromInrRate).setScale(4, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<String> fetchActiveSupportedCurrencies() {
        List<String> currencies = new ArrayList<>(
                supportedCurrencyRepository.findAll().stream()
                        .filter(SupportedCurrency::isActive)
                        .map(SupportedCurrency::getCode)
                        .toList()
        );

        if (!currencies.contains("INR")) {
            currencies.add("INR");
        }

        return currencies;
    }

    public List<CountryResponse> fetchAllCountries() {
        return Arrays.stream(Locale.getISOCountries())
                .map(code -> new CountryResponse(code, Locale.of("", code).getDisplayCountry(Locale.ENGLISH)))
                .filter(country -> country.name() != null && !country.name().isBlank())
                .sorted(Comparator.comparing(CountryResponse::name))
                .toList();
    }

    public CountryDefaultsResponse resolveCountryDefaults(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return new CountryDefaultsResponse(null, List.of("UTC"));
        }

        String formattedCode = countryCode.trim().toUpperCase();
        String currency = null;

        try {
            Locale locale = Locale.of("", formattedCode);
            Currency javaCurrency = Currency.getInstance(locale);
            if (javaCurrency != null) {
                currency = javaCurrency.getCurrencyCode();
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid country code for currency resolution: " + formattedCode);
        }

        String[] icuTimezones = TimeZone.getAvailableIDs(formattedCode);
        List<String> timezones = (icuTimezones != null && icuTimezones.length > 0)
                ? List.of(icuTimezones)
                : List.of("UTC");

        return new CountryDefaultsResponse(currency, timezones);
    }

    private BigDecimal getInrRate(String currency) {
        if (currency.equalsIgnoreCase("INR")) {
            return BigDecimal.ONE;
        }

        ExchangeRate rate = exchangeRateRepository.findByBaseCurrencyAndQuoteCurrency("INR", currency)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "FOREX_RATE_NOT_FOUND", "Exchange rate not found for INR to " + currency));

        return rate.getRate();
    }
}