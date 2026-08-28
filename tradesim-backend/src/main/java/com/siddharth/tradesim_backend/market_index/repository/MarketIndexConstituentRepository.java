package com.siddharth.tradesim_backend.market_index.repository;

import com.siddharth.tradesim_backend.market_index.model.MarketIndexConstituent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MarketIndexConstituentRepository extends JpaRepository<MarketIndexConstituent, UUID> {
    List<MarketIndexConstituent> findByIndexId(UUID indexId);
    List<MarketIndexConstituent> findByStockId(UUID stockId);
    boolean existsByIndexIdAndStockId(UUID indexId, UUID stockId);
}