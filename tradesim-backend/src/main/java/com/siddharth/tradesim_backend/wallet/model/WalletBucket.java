package com.siddharth.tradesim_backend.wallet.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "wallet_buckets", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"wallet_id", "currency"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletBucket extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    public BigDecimal getAvailableBalance() {
        return this.balance.subtract(this.lockedBalance);
    }
}