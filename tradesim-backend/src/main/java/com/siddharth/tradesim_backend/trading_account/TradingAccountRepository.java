package com.siddharth.tradesim_backend.trading_account;

import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TradingAccountRepository extends JpaRepository<TradingAccount, UUID> {
    Optional<TradingAccount> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}