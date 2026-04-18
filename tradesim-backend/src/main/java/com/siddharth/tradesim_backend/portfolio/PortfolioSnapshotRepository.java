package com.siddharth.tradesim_backend.portfolio;

import com.siddharth.tradesim_backend.portfolio.model.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, UUID> {
    List<PortfolioSnapshot> findByUserIdOrderBySnapshotDate(UUID userId);
    Optional<PortfolioSnapshot> findByUserIdAndSnapshotDate(UUID userId, LocalDate snapshotDate);
}