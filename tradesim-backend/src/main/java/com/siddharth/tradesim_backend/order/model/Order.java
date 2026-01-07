package com.siddharth.tradesim_backend.order.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_order_user", columnList = "user_id"),
                @Index(name = "idx_order_stock", columnList = "stock_id"),
                @Index(name = "idx_order_status", columnList = "status"),
                @Index(name = "idx_order_created", columnList = "created_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends AuditableEntity {

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
    private OrderSide side; // BUY / SELL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType; // MARKET / LIMIT

    @Column(nullable = false)
    private int quantity;

    @Column(name = "remaining_quantity", nullable = false)
    private int remainingQuantity;

    @Column(precision = 19, scale = 4)
    private BigDecimal limitPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
}