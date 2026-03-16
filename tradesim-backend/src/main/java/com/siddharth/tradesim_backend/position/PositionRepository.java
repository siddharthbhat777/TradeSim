package com.siddharth.tradesim_backend.position;

import com.siddharth.tradesim_backend.position.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PositionRepository extends JpaRepository<Position, UUID> {
    List<Position> findByUserId(UUID userId);
    Optional<Position> findByUserIdAndStockId(UUID userId, UUID stockId);
}