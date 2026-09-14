package com.siddharth.tradesim_backend.forex.service;

import com.siddharth.tradesim_backend.forex.enums.CurrencyCategory;
import com.siddharth.tradesim_backend.forex.model.FxFeeSchedule;
import com.siddharth.tradesim_backend.forex.model.SupportedCurrency;
import com.siddharth.tradesim_backend.forex.repository.FxFeeScheduleRepository;
import com.siddharth.tradesim_backend.forex.repository.SupportedCurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class FxFeeService {
    private final SupportedCurrencyRepository supportedCurrencyRepository;
    private final FxFeeScheduleRepository fxFeeScheduleRepository;

    private static final BigDecimal DEFAULT_EXOTIC_FEE = BigDecimal.valueOf(0.0100);

    @Transactional
    public BigDecimal calculateConversionFee(String sourceCurrencyCode, String targetCurrencyCode, BigDecimal amountToConvert) {
        if (sourceCurrencyCode.equalsIgnoreCase(targetCurrencyCode)) {
            return BigDecimal.ZERO;
        }

        CurrencyCategory sourceCategory = getOrCreateCategory(sourceCurrencyCode);
        CurrencyCategory targetCategory = getOrCreateCategory(targetCurrencyCode);

        BigDecimal feePercentage = fxFeeScheduleRepository
                .findBySourceCategoryAndTargetCategory(sourceCategory, targetCategory)
                .map(FxFeeSchedule::getFeePercentage)
                .orElse(DEFAULT_EXOTIC_FEE);

        return amountToConvert.multiply(feePercentage).setScale(4, RoundingMode.HALF_UP);
    }

    private CurrencyCategory getOrCreateCategory(String currencyCode) {
        return supportedCurrencyRepository.findById(currencyCode)
                .map(SupportedCurrency::getCategory)
                .orElseGet(() -> {
                    SupportedCurrency newCurrency = SupportedCurrency.builder()
                            .code(currencyCode)
                            .category(CurrencyCategory.EXOTIC)
                            .isActive(true)
                            .build();
                    supportedCurrencyRepository.save(newCurrency);
                    return CurrencyCategory.EXOTIC;
                });
    }
}