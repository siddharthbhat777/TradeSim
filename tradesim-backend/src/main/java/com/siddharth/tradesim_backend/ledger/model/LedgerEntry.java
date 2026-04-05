package com.siddharth.tradesim_backend.ledger.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import com.siddharth.tradesim_backend.ledger.enums.LedgerEntryType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "ledger_entries",
        indexes = {
                @Index(name = "idx_ledger_trading_account", columnList = "trading_account_id"),
                @Index(name = "idx_ledger_user", columnList = "user_id"),
                @Index(name = "idx_ledger_stock", columnList = "stock_id"),
                @Index(name = "idx_ledger_order", columnList = "order_id"),
                @Index(name = "idx_ledger_type", columnList = "type"),
                @Index(name = "idx_ledger_created", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID tradingAccountId;

    @Column(nullable = false)
    private UUID userId;

    private UUID stockId;

    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerEntryType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal lockedBalanceAfter;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal marginLoanAfter;

    @Column(length = 500)
    private String description;
}