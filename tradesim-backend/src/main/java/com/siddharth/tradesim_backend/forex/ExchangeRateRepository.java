package com.siddharth.tradesim_backend.forex;

import com.siddharth.tradesim_backend.forex.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {
    Optional<ExchangeRate> findByBaseCurrencyAndQuoteCurrency(String baseCurrency, String quoteCurrency);
}