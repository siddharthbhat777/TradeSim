package com.siddharth.tradesim_backend.portfolio.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "portfolio_snapshots",
        indexes = {
                @Index(name = "idx_snapshot_user", columnList = "user_id"),
                @Index(name = "idx_snapshot_date", columnList = "snapshot_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalValue;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal realizedPnl;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal unrealizedPnl;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal equity;
}