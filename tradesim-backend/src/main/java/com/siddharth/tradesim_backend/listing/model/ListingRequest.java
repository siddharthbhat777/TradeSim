package com.siddharth.tradesim_backend.listing.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import com.siddharth.tradesim_backend.listing.enums.ListingStatus;
import com.siddharth.tradesim_backend.stock.enums.Sector;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "listing_requests",
        indexes = {
                @Index(name = "idx_listing_company", columnList = "company_id"),
                @Index(name = "idx_listing_symbol", columnList = "symbol"),
                @Index(name = "idx_listing_status", columnList = "status"),
                @Index(name = "idx_listing_submitted_by", columnList = "submitted_by_user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingRequest extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID submittedByUserId;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(nullable = false)
    private UUID exchangeId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal referencePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sector sector;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal priceBandPercent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatus status;

    private UUID reviewedByUserId;

    private Instant reviewedAt;

    private UUID approvedStockId;

    @Column(length = 500)
    private String rejectionReason;
}