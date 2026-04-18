package com.siddharth.tradesim_backend.ipo.repository;

import com.siddharth.tradesim_backend.ipo.model.IpoSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IpoSubscriptionRepository extends JpaRepository<IpoSubscription, UUID> {
    boolean existsByIpoOfferIdAndUserId(UUID ipoOfferId, UUID userId);
    List<IpoSubscription> findByIpoOfferIdOrderByCreatedAtAsc(UUID ipoOfferId);
    List<IpoSubscription> findByUserIdOrderByCreatedAtDesc(UUID userId);
}