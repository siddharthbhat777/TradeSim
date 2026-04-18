package com.siddharth.tradesim_backend.ipo.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import com.siddharth.tradesim_backend.ipo.enums.IpoOfferStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "ipo_offers",
        indexes = {
                @Index(name = "idx_ipo_offer_company", columnList = "company_id"),
                @Index(name = "idx_ipo_offer_stock", columnList = "stock_id"),
                @Index(name = "idx_ipo_offer_status", columnList = "status"),
                @Index(name = "idx_ipo_offer_submitted_by", columnList = "submitted_by_user_id"),
                @Index(name = "idx_ipo_offer_start", columnList = "subscription_start_at"),
                @Index(name = "idx_ipo_offer_end", columnList = "subscription_end_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpoOffer extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID stockId;

    @Column(nullable = false)
    private UUID submittedByUserId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal issuePrice;

    @Column(nullable = false)
    private int sharesPerAllottee;

    @Column(nullable = false)
    private int maxAllottees;

    @Column(nullable = false)
    private Instant subscriptionStartAt;

    @Column(nullable = false)
    private Instant subscriptionEndAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IpoOfferStatus status;

    private UUID reviewedByUserId;

    private Instant reviewedAt;

    private UUID finalizedByUserId;

    private Instant finalizedAt;

    @Column(length = 500)
    private String rejectionReason;
}