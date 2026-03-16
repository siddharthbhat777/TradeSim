package com.siddharth.tradesim_backend.auth.model;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@Builder
public class User extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, precision = 19, scale = 4)
    @Getter(AccessLevel.NONE)
    private BigDecimal balance;

    @Column(nullable = false, precision = 19, scale = 4)
    @Getter(AccessLevel.NONE)
    private BigDecimal lockedBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus accountStatus;

    private Instant lastLogin;

    public BigDecimal getAvailableBalance() {
        return balance.subtract(lockedBalance);
    }

    public void debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        if (getAvailableBalance().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient balance");
        }
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        this.balance = this.balance.add(amount);
    }

    public void lockFunds(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Lock amount must be positive");
        }
        if (getAvailableBalance().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient available balance");
        }
        this.lockedBalance = this.lockedBalance.add(amount);
    }

    public void unlockFunds(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unlock amount must be positive");
        }

        if (lockedBalance.compareTo(amount) < 0) {
            throw new BusinessException("Cannot unlock more than locked balance");
        }

        this.lockedBalance = this.lockedBalance.subtract(amount);
    }

    public BigDecimal calculateEquity(BigDecimal unrealizedPnl) {
        if (unrealizedPnl == null) {
            throw new IllegalArgumentException("Unrealized PnL cannot be null");
        }

        return balance.add(unrealizedPnl);
    }
}