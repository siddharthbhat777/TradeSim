package com.siddharth.tradesim_backend.forex.service;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.forex.model.ExchangeRate;
import com.siddharth.tradesim_backend.forex.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ForexService {
    private final ExchangeRateRepository exchangeRateRepository;

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

    private BigDecimal getInrRate(String currency) {
        if (currency.equalsIgnoreCase("INR")) {
            return BigDecimal.ONE;
        }

        ExchangeRate rate = exchangeRateRepository.findByBaseCurrencyAndQuoteCurrency("INR", currency)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "FOREX_RATE_NOT_FOUND", "Exchange rate not found for INR to " + currency));

        return rate.getRate();
    }
}