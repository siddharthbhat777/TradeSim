package com.siddharth.tradesim_backend.holding.models;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
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
    private int quantity;
}