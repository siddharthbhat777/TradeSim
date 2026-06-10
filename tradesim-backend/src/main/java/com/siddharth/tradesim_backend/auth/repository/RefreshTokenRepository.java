package com.siddharth.tradesim_backend.auth.repository;

import com.siddharth.tradesim_backend.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            UPDATE RefreshToken rt
            SET rt.revokedAt = :revokedAt
            WHERE rt.user.id = :userId
              AND rt.revokedAt IS NULL
              AND rt.expiresAt > :revokedAt
            """)
    void revokeActiveTokensForUser(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);
}