package com.siddharth.tradesim_backend.ipo.repository;

import com.siddharth.tradesim_backend.ipo.enums.IpoOfferStatus;
import com.siddharth.tradesim_backend.ipo.model.IpoOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IpoOfferRepository extends JpaRepository<IpoOffer, UUID> {
    boolean existsByStockIdAndStatusIn(UUID stockId, List<IpoOfferStatus> statuses);
    List<IpoOffer> findByStatusOrderByCreatedAtAsc(IpoOfferStatus status);
}