package com.siddharth.tradesim_backend.issuance;

import com.siddharth.tradesim_backend.issuance.enums.IssuanceStatus;
import com.siddharth.tradesim_backend.issuance.model.IssuanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssuanceRequestRepository extends JpaRepository<IssuanceRequest, UUID> {
    boolean existsByStockIdAndStatus(UUID stockId, IssuanceStatus status);
    List<IssuanceRequest> findByStatusOrderByCreatedAtAsc(IssuanceStatus status);
}