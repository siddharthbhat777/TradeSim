package com.siddharth.tradesim_backend.listing.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "listing_cap_table_entries",
        indexes = {
                @Index(name = "idx_cap_table_listing", columnList = "listing_request_id"),
                @Index(name = "idx_cap_table_user", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cap_table_listing_user", columnNames = {"listing_request_id", "user_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingCapTableEntry extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_request_id", nullable = false)
    private ListingRequest listingRequest;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private int quantity;
}