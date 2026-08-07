package com.siddharth.tradesim_backend.forex.repository;

import com.siddharth.tradesim_backend.forex.model.SupportedCurrency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportedCurrencyRepository extends JpaRepository<SupportedCurrency, String> {
}