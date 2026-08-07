package com.siddharth.tradesim_backend.forex.service;

import com.siddharth.tradesim_backend.forex.model.ExchangeRate;
import com.siddharth.tradesim_backend.forex.model.dto.FrankfurterRate;
import com.siddharth.tradesim_backend.forex.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForexIntegrationServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient restClient;

    @InjectMocks
    private ForexIntegrationService forexIntegrationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(forexIntegrationService, "restClient", restClient);
        ReflectionTestUtils.setField(forexIntegrationService, "forexApiUrl", "https://mock-api.com/latest");
    }

    @Test
    void fetchAndStoreExchangeRates_WithValidData() {
        FrankfurterRate newRate = mock(FrankfurterRate.class);
        when(newRate.base()).thenReturn("INR");
        when(newRate.quote()).thenReturn("USD");
        when(newRate.rate()).thenReturn(new BigDecimal("0.012"));

        FrankfurterRate existingRateDto = mock(FrankfurterRate.class);
        when(existingRateDto.base()).thenReturn("INR");
        when(existingRateDto.quote()).thenReturn("EUR");
        when(existingRateDto.rate()).thenReturn(new BigDecimal("0.011"));

        List<FrankfurterRate> mockResponse = List.of(newRate, existingRateDto);

        ExchangeRate existingRateEntity = ExchangeRate.builder()
                .baseCurrency("INR")
                .quoteCurrency("EUR")
                .rate(new BigDecimal("0.010"))
                .build();

        when(restClient.get()
                .uri(anyString())
                .retrieve()
                .body(ArgumentMatchers.<ParameterizedTypeReference<List<FrankfurterRate>>>any()))
                .thenReturn(mockResponse);

        when(exchangeRateRepository.findByBaseCurrencyAndQuoteCurrency("INR", "USD"))
                .thenReturn(Optional.empty());
        when(exchangeRateRepository.findByBaseCurrencyAndQuoteCurrency("INR", "EUR"))
                .thenReturn(Optional.of(existingRateEntity));

        forexIntegrationService.fetchAndStoreExchangeRates();

        verify(exchangeRateRepository, times(2)).save(any(ExchangeRate.class));
    }

    @Test
    void fetchAndStoreExchangeRates_WithNullResponse() {
        when(restClient.get()
                .uri(anyString())
                .retrieve()
                .body(ArgumentMatchers.<ParameterizedTypeReference<List<FrankfurterRate>>>any()))
                .thenReturn(null);

        forexIntegrationService.fetchAndStoreExchangeRates();

        verifyNoInteractions(exchangeRateRepository);
    }
}