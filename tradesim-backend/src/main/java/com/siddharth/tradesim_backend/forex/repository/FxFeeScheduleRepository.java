package com.siddharth.tradesim_backend.forex.repository;

import com.siddharth.tradesim_backend.forex.enums.CurrencyCategory;
import com.siddharth.tradesim_backend.forex.model.FxFeeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FxFeeScheduleRepository extends JpaRepository<FxFeeSchedule, UUID> {
    Optional<FxFeeSchedule> findBySourceCategoryAndTargetCategory(CurrencyCategory source, CurrencyCategory target);
}