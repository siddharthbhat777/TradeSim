package com.siddharth.tradesim_backend.holding.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "holdings",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "stock_id"})
        },
        indexes = {
                @Index(name = "idx_holding_user", columnList = "user_id"),
                @Index(name = "idx_holding_stock", columnList = "stock_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holding extends AuditableEntity {
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
}