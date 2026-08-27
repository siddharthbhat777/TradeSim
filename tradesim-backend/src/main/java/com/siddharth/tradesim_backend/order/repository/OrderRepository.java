package com.siddharth.tradesim_backend.order.repository;

import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByStatusIn(Collection<OrderStatus> statuses);
    List<Order> findByStockIdAndStatusIn(UUID stockId, List<OrderStatus> statuses);
    List<Order> findByUserIdAndStatusIn(UUID userId, List<OrderStatus> statuses);
    List<Order> findByStatusInAndExpiresAtLessThanEqual(Collection<OrderStatus> statuses, Instant expiresAt);
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);
}