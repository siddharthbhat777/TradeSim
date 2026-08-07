package com.siddharth.tradesim_backend.forex.service;

import com.siddharth.tradesim_backend.forex.enums.CurrencyCategory;
import com.siddharth.tradesim_backend.forex.model.FxFeeSchedule;
import com.siddharth.tradesim_backend.forex.model.SupportedCurrency;
import com.siddharth.tradesim_backend.forex.repository.FxFeeScheduleRepository;
import com.siddharth.tradesim_backend.forex.repository.SupportedCurrencyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FxFeeServiceTest {

    @Mock
    private SupportedCurrencyRepository supportedCurrencyRepository;

    @Mock
    private FxFeeScheduleRepository fxFeeScheduleRepository;

    @InjectMocks
    private FxFeeService fxFeeService;

    @Test
    void calculateConversionFee_SameCurrency() {
        BigDecimal fee = fxFeeService.calculateConversionFee("USD", "USD", BigDecimal.valueOf(100));
        assertEquals(BigDecimal.ZERO, fee);
        verifyNoInteractions(supportedCurrencyRepository, fxFeeScheduleRepository);
    }

    @Test
    void calculateConversionFee_KnownCurrencies_ScheduleExists() {
        SupportedCurrency source = SupportedCurrency.builder().code("USD").category(CurrencyCategory.MAJOR).build();
        SupportedCurrency target = SupportedCurrency.builder().code("INR").category(CurrencyCategory.EXOTIC).build();
        FxFeeSchedule schedule = FxFeeSchedule.builder().feePercentage(new BigDecimal("0.0050")).build();

        when(supportedCurrencyRepository.findById("USD")).thenReturn(Optional.of(source));
        when(supportedCurrencyRepository.findById("INR")).thenReturn(Optional.of(target));
        when(fxFeeScheduleRepository.findBySourceCategoryAndTargetCategory(CurrencyCategory.MAJOR, CurrencyCategory.EXOTIC))
                .thenReturn(Optional.of(schedule));

        BigDecimal fee = fxFeeService.calculateConversionFee("USD", "INR", BigDecimal.valueOf(1000));
        assertEquals(new BigDecimal("5.0000"), fee);
    }

    @Test
    void calculateConversionFee_UnknownCurrency_SavesAsExotic() {
        SupportedCurrency target = SupportedCurrency.builder().code("USD").category(CurrencyCategory.MAJOR).build();
        FxFeeSchedule schedule = FxFeeSchedule.builder().feePercentage(new BigDecimal("0.0075")).build();

        when(supportedCurrencyRepository.findById("NEW")).thenReturn(Optional.empty());
        when(supportedCurrencyRepository.findById("USD")).thenReturn(Optional.of(target));
        when(supportedCurrencyRepository.save(any(SupportedCurrency.class))).thenAnswer(i -> i.getArgument(0));
        when(fxFeeScheduleRepository.findBySourceCategoryAndTargetCategory(CurrencyCategory.EXOTIC, CurrencyCategory.MAJOR))
                .thenReturn(Optional.of(schedule));

        BigDecimal fee = fxFeeService.calculateConversionFee("NEW", "USD", BigDecimal.valueOf(1000));

        assertEquals(new BigDecimal("7.5000"), fee);
        verify(supportedCurrencyRepository).save(argThat(c -> c.getCode().equals("NEW") && c.getCategory() == CurrencyCategory.EXOTIC));
    }

    @Test
    void calculateConversionFee_MissingSchedule_UsesDefault() {
        SupportedCurrency source = SupportedCurrency.builder().code("USD").category(CurrencyCategory.MAJOR).build();
        SupportedCurrency target = SupportedCurrency.builder().code("INR").category(CurrencyCategory.EXOTIC).build();

        when(supportedCurrencyRepository.findById("USD")).thenReturn(Optional.of(source));
        when(supportedCurrencyRepository.findById("INR")).thenReturn(Optional.of(target));
        when(fxFeeScheduleRepository.findBySourceCategoryAndTargetCategory(CurrencyCategory.MAJOR, CurrencyCategory.EXOTIC))
                .thenReturn(Optional.empty());

        BigDecimal fee = fxFeeService.calculateConversionFee("USD", "INR", BigDecimal.valueOf(1000));
        assertEquals(new BigDecimal("10.0000"), fee);
    }
}