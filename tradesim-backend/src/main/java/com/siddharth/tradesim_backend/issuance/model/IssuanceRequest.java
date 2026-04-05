package com.siddharth.tradesim_backend.issuance.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import com.siddharth.tradesim_backend.issuance.enums.IssuanceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "issuance_requests",
        indexes = {
                @Index(name = "idx_issuance_company", columnList = "company_id"),
                @Index(name = "idx_issuance_stock", columnList = "stock_id"),
                @Index(name = "idx_issuance_status", columnList = "status"),
                @Index(name = "idx_issuance_submitted_by", columnList = "submitted_by_user_id"),
                @Index(name = "idx_issuance_liquidity_provider", columnList = "liquidity_provider_user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssuanceRequest extends AuditableEntity {
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

    @Column(nullable = false)
    private int totalIssuedShares;

    @Column(nullable = false)
    private int tradableFloatShares;

    @Column(nullable = false)
    private UUID liquidityProviderUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssuanceStatus status;

    private UUID reviewedByUserId;

    private Instant reviewedAt;

    @Column(length = 500)
    private String rejectionReason;
}