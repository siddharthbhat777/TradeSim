package com.siddharth.tradesim_backend.position;

import com.siddharth.tradesim_backend.position.model.Position;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PositionRepository extends JpaRepository<Position, UUID> {
    List<Position> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Position> findByUserIdAndStockId(UUID userId, UUID stockId);

    @Query("SELECT p FROM Position p WHERE p.userId = :userId AND p.stockId = :stockId")
    Optional<Position> findUnlockedByUserIdAndStockId(@Param("userId") UUID userId, @Param("stockId") UUID stockId);
}