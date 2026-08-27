package com.siddharth.tradesim_backend.order.repository;

import com.siddharth.tradesim_backend.order.model.Fill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FillRepository extends JpaRepository<Fill, UUID> {
    @Query("SELECT f FROM Fill f WHERE f.buyOrderId IN :orderIds OR f.sellOrderId IN :orderIds")
    List<Fill> findFillsByOrderIds(@Param("orderIds") List<UUID> orderIds);
}