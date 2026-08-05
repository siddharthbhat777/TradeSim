package com.siddharth.tradesim_backend.forex.service;

import com.siddharth.tradesim_backend.forex.model.ExchangeRate;
import com.siddharth.tradesim_backend.forex.model.dto.FrankfurterRate;
import com.siddharth.tradesim_backend.forex.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ForexIntegrationService {
    private final ExchangeRateRepository exchangeRateRepository;
    private final RestClient restClient = RestClient.create();

    @Value("${forex.api.url}")
    private String forexApiUrl;

    @Transactional
    public void fetchAndStoreExchangeRates() {
        List<FrankfurterRate> rates = restClient.get()
                .uri(forexApiUrl + "?base=INR")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (rates != null) {
            for (FrankfurterRate frankfurterRate : rates) {
                ExchangeRate rate = exchangeRateRepository
                        .findByBaseCurrencyAndQuoteCurrency(frankfurterRate.base(), frankfurterRate.quote())
                        .orElseGet(() -> ExchangeRate.builder()
                                .baseCurrency(frankfurterRate.base())
                                .quoteCurrency(frankfurterRate.quote())
                                .build());

                rate.setRate(frankfurterRate.rate());
                exchangeRateRepository.save(rate);
            }
        }
    }
}