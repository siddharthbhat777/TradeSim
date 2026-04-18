package com.siddharth.tradesim_backend.listing;

import com.siddharth.tradesim_backend.listing.enums.ListingStatus;
import com.siddharth.tradesim_backend.listing.model.ListingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ListingRequestRepository extends JpaRepository<ListingRequest, UUID> {
    boolean existsBySymbolAndStatus(String symbol, ListingStatus status);
    List<ListingRequest> findByStatusOrderByCreatedAtAsc(ListingStatus status);
}