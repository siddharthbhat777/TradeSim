package com.siddharth.tradesim_backend.wallet.repository;

import com.siddharth.tradesim_backend.wallet.model.WalletBucket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletBucketRepository extends JpaRepository<WalletBucket, UUID> {
    List<WalletBucket> findByWalletId(UUID walletId);

    Optional<WalletBucket> findByWalletIdAndCurrency(UUID walletId, String currency);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT wb FROM WalletBucket wb WHERE wb.wallet.id = :walletId AND wb.currency = :currency")
    Optional<WalletBucket> findByWalletIdAndCurrencyForUpdate(UUID walletId, String currency);
}