package com.siddharth.tradesim_backend.ledger;

import com.siddharth.tradesim_backend.ledger.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findByUserIdOrderByCreatedAtDesc(UUID userId);
}