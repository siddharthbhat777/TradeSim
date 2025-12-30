package com.siddharth.tradesim_backend.holding;

import com.siddharth.tradesim_backend.holding.models.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, UUID> {
    List<Holding> findByUserId(UUID userId);
}