package com.siddharth.tradesim_backend.trading_account.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import com.siddharth.tradesim_backend.trading_account.TradingAccountException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "trading_accounts",
        indexes = {
                @Index(name = "idx_trading_account_user", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_trading_account_user", columnNames = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradingAccount extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String baseCurrency = "INR";

    @Column(nullable = false, precision = 19, scale = 4)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private BigDecimal marginLoan = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private int leverage = 5;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal maintenanceMarginPercent = BigDecimal.valueOf(25);

    public BigDecimal getMarginLoan() {
        if (marginLoan == null) {
            return BigDecimal.ZERO;
        }
        if (marginLoan.compareTo(BigDecimal.ZERO) < 0) {
            throw TradingAccountException.conflict("Margin loan cannot be negative");
        }
        return marginLoan;
    }

    public void increaseMarginLoan(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Margin loan increase must be positive");
        }
        this.marginLoan = getMarginLoan().add(amount);
    }

    public void decreaseMarginLoan(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Margin loan decrease must be positive");
        }
        if (getMarginLoan().compareTo(amount) < 0) {
            throw TradingAccountException.conflict("Cannot repay more than margin loan");
        }
        this.marginLoan = getMarginLoan().subtract(amount);
    }
}