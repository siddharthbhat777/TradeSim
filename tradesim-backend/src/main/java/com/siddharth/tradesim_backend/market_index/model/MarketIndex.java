package com.siddharth.tradesim_backend.market_index.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "market_indices",
        indexes = {
                @Index(name = "idx_market_index_symbol", columnList = "symbol", unique = true),
                @Index(name = "idx_market_index_exchange", columnList = "exchange_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketIndex extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(nullable = false)
    private UUID exchangeId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal baseValue;

    @Column(precision = 19, scale = 4)
    private BigDecimal baseMarketCap;

    @Column(precision = 19, scale = 4)
    private BigDecimal currentValue;

    @Column(precision = 19, scale = 4)
    private BigDecimal dayOpen;

    @Column(precision = 19, scale = 4)
    private BigDecimal dayHigh;

    @Column(precision = 19, scale = 4)
    private BigDecimal dayLow;

    @Column(precision = 19, scale = 4)
    private BigDecimal previousClose;

    private LocalDate lastTradingDate;
}