package com.siddharth.tradesim_backend.order.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "fills",
        indexes = {
                @Index(name = "idx_fill_order", columnList = "order_id"),
                @Index(name = "idx_fill_stock", columnList = "stock_id"),
                @Index(name = "idx_fill_created", columnList = "executed_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "stock_id", nullable = false)
    private UUID stockId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;
}