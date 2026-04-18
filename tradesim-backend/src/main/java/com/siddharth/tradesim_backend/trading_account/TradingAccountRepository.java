package com.siddharth.tradesim_backend.trading_account;

import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TradingAccountRepository extends JpaRepository<TradingAccount, UUID> {
    Optional<TradingAccount> findByUserId(UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tradingAccount from TradingAccount tradingAccount where tradingAccount.userId = :userId")
    Optional<TradingAccount> findByUserIdForUpdate(@Param("userId") UUID userId);
    boolean existsByUserId(UUID userId);
}