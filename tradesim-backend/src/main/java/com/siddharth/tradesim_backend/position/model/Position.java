package com.siddharth.tradesim_backend.position.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import com.siddharth.tradesim_backend.position.PositionException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "positions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "stock_id"})
        },
        indexes = {
                @Index(name = "idx_position_user", columnList = "user_id"),
                @Index(name = "idx_position_stock", columnList = "stock_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "stock_id", nullable = false)
    private UUID stockId;

    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private int quantity;

    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private int lockedQuantity;

    @Column(nullable = false, precision = 19, scale = 4)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private BigDecimal averageBuyPrice = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 4)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private BigDecimal totalInvested = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 4)
    @Setter(AccessLevel.NONE)
    private BigDecimal realizedPnl;

    public int getAvailableQuantity() {
        return quantity - lockedQuantity;
    }

    public void lockShares(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Lock quantity must be positive");
        }

        if (getAvailableQuantity() < quantity) {
            throw PositionException.conflict("Insufficient available shares to lock");
        }

        this.lockedQuantity += quantity;
    }

    public void unlockShares(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Unlock amount must be positive");
        }

        if (lockedQuantity < quantity) {
            throw PositionException.conflict("Cannot unlock more shares than locked");
        }

        this.lockedQuantity -= quantity;
    }

    public void addInvestment(BigDecimal exactBlockCost, int quantityToAdd) {
        if (quantityToAdd <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (exactBlockCost == null || exactBlockCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cost must be positive");
        }

        this.totalInvested = this.totalInvested.add(exactBlockCost);
        this.quantity += quantityToAdd;
        this.averageBuyPrice = this.totalInvested.divide(BigDecimal.valueOf(this.quantity), 4, RoundingMode.HALF_UP);
    }

    public void decreaseQuantity(int quantityToSubtract) {
        if (quantityToSubtract <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (this.quantity < quantityToSubtract) {
            throw PositionException.conflict("Insufficient shares");
        }

        BigDecimal costReduction = this.totalInvested.multiply(BigDecimal.valueOf(quantityToSubtract))
                .divide(BigDecimal.valueOf(this.quantity), 4, RoundingMode.HALF_UP);

        this.totalInvested = this.totalInvested.subtract(costReduction);
        this.quantity -= quantityToSubtract;

        if (this.quantity == 0) {
            this.totalInvested = BigDecimal.ZERO;
            this.averageBuyPrice = BigDecimal.ZERO;
        } else {
            this.averageBuyPrice = this.totalInvested.divide(BigDecimal.valueOf(this.quantity), 4, RoundingMode.HALF_UP);
        }
    }

    public void addRealizedPnl(BigDecimal pnl) {
        if (pnl == null) {
            throw new IllegalArgumentException("PnL cannot be null");
        }

        this.realizedPnl = this.realizedPnl.add(pnl);
    }
}