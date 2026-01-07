package com.siddharth.tradesim_backend.order;

import com.siddharth.tradesim_backend.order.enums.Status;
import com.siddharth.tradesim_backend.order.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeRepository extends JpaRepository<Trade, UUID> {
    List<Trade> findByStatus(Status status);
    List<Trade> findByUserIdAndStatus(UUID userId, Status status);
    List<Trade> findByStockIdAndStatus(UUID stockId, Status status);
}