package com.siddharth.tradesim_backend.market_index.repository;

import com.siddharth.tradesim_backend.market_index.model.MarketIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MarketIndexRepository extends JpaRepository<MarketIndex, UUID> {
    boolean existsBySymbol(String symbol);
    List<MarketIndex> findByExchangeId(UUID exchangeId);
}