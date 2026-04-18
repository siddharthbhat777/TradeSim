package com.siddharth.tradesim_backend.ipo.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import com.siddharth.tradesim_backend.ipo.enums.IpoSubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "ipo_subscriptions",
        indexes = {
                @Index(name = "idx_ipo_subscription_offer", columnList = "ipo_offer_id"),
                @Index(name = "idx_ipo_subscription_user", columnList = "user_id"),
                @Index(name = "idx_ipo_subscription_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ipo_subscription_offer_user", columnNames = {"ipo_offer_id", "user_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpoSubscription extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID ipoOfferId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal lockedAmount;

    @Column(nullable = false)
    private int allottedShares;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IpoSubscriptionStatus status;
}