package com.siddharth.tradesim_backend.position.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
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
    private BigDecimal averageBuyPrice;

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
            throw new BusinessException("Insufficient available shares to lock");
        }

        this.lockedQuantity += quantity;
    }

    public void unlockShares(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Unlock amount must be positive");
        }

        if (lockedQuantity < quantity) {
            throw new BusinessException("Cannot unlock more shares than locked");
        }

        this.lockedQuantity -= quantity;
    }

    public void increaseQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.quantity += quantity;
    }

    public void decreaseQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (this.quantity < quantity) {
            throw new BusinessException("Insufficient shares");
        }

        this.quantity -= quantity;
    }

    public void updateAverageBuyPrice(BigDecimal executionPrice, int executedQty) {
        if (executedQty <= 0) {
            throw new IllegalArgumentException("Executed quantity must be positive");
        }

        BigDecimal totalCost = this.averageBuyPrice.multiply(BigDecimal.valueOf(this.quantity));
        BigDecimal newCost = executionPrice.multiply(BigDecimal.valueOf(executedQty));

        BigDecimal newTotalCost = totalCost.add(newCost);
        int newTotalQuantity = this.quantity + executedQty;

        this.averageBuyPrice = newTotalCost.divide(BigDecimal.valueOf(newTotalQuantity), 4, java.math.RoundingMode.HALF_UP);
    }

    public void addRealizedPnl(BigDecimal pnl) {
        if (pnl == null) {
            throw new IllegalArgumentException("PnL cannot be null");
        }

        this.realizedPnl = this.realizedPnl.add(pnl);
    }
}