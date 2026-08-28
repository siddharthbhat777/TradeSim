package com.siddharth.tradesim_backend.market_index.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "market_index_constituents",
        indexes = {
                @Index(name = "idx_market_index_const_index", columnList = "index_id"),
                @Index(name = "idx_market_index_const_stock", columnList = "stock_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_market_index_const", columnNames = {"index_id", "stock_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketIndexConstituent extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID indexId;

    @Column(nullable = false)
    private UUID stockId;
}