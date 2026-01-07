package com.siddharth.tradesim_backend.order.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.enums.Status;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "trades",
        indexes = {
                @Index(name = "idx_trade_user", columnList = "user_id"),
                @Index(name = "idx_trade_stock", columnList = "stock_id"),
                @Index(name = "idx_trade_created", columnList = "created_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "stock_id", nullable = false)
    private UUID stockId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide type;

    @Column(nullable = false)
    private int quantity;

    @Column(precision = 19, scale = 4)
    private BigDecimal priceAtExecution;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;

    @Column(precision = 19, scale = 4)
    private BigDecimal limitPrice;

    @Column(name = "executed_at")
    private Instant executedAt;
}