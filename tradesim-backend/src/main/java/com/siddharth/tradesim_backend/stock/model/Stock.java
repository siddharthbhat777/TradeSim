package com.siddharth.tradesim_backend.stock.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import com.siddharth.tradesim_backend.stock.enums.Sector;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "stocks",
        indexes = {
                @Index(name = "idx_stock_symbol", columnList = "symbol", unique = true),
                @Index(name = "idx_stock_sector", columnList = "sector"),
                @Index(name = "idx_stock_exchange", columnList = "exchange_id"),
                @Index(name = "idx_stock_company", columnList = "company_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 10)
    private String symbol;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID exchangeId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal lastTradedPrice;

    @Column(nullable = false)
    private Long totalVolume;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sector sector;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockStatus status;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal priceBandPercent;

    private Integer totalIssuedShares;

    private Integer tradableFloatShares;

    @Column(precision = 19, scale = 4)
    private BigDecimal dayOpen;

    @Column(precision = 19, scale = 4)
    private BigDecimal dayHigh;

    @Column(precision = 19, scale = 4)
    private BigDecimal dayLow;

    private Long dayVolume;

    private LocalDate lastTradingDate;
}