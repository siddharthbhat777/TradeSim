package com.siddharth.tradesim_backend.wallet.repository;

import com.siddharth.tradesim_backend.wallet.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    Optional<Wallet> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}