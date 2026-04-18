package com.siddharth.tradesim_backend.exchange;

import com.siddharth.tradesim_backend.exchange.model.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExchangeRepository extends JpaRepository<Exchange, UUID> {
    boolean existsByName(String name);
    boolean existsByCode(String code);
    Optional<Exchange> findByCode(String code);
}