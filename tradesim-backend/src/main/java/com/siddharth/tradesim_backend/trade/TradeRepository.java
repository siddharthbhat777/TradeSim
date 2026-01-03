package com.siddharth.tradesim_backend.trade;

import com.siddharth.tradesim_backend.trade.enums.Status;
import com.siddharth.tradesim_backend.trade.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeRepository extends JpaRepository<Trade, UUID> {
    List<Trade> findByStatus(Status status);
}