package com.siddharth.tradesim_backend.wallet.service;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.wallet.model.Wallet;
import com.siddharth.tradesim_backend.wallet.model.WalletBucket;
import com.siddharth.tradesim_backend.wallet.repository.WalletBucketRepository;
import com.siddharth.tradesim_backend.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletBucketRepository walletBucketRepository;

    @Transactional
    public Wallet createWalletForUser(UUID userId, String baseCurrency) {
        if (walletRepository.existsByUserId(userId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "WALLET_EXISTS", "Wallet already exists for this user");
        }

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .build();

        Wallet savedWallet = walletRepository.save(wallet);

        WalletBucket baseBucket = WalletBucket.builder()
                .wallet(savedWallet)
                .currency(baseCurrency != null ? baseCurrency : "INR")
                .balance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .build();

        walletBucketRepository.save(baseBucket);

        return savedWallet;
    }

    @Transactional(readOnly = true)
    public Wallet getWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", "Wallet not found"));
    }

    @Transactional
    public WalletBucket getBucketForUpdate(UUID walletId, String currency) {
        return walletBucketRepository.findByWalletIdAndCurrencyForUpdate(walletId, currency)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "BUCKET_NOT_FOUND", "Currency bucket not found"));
    }

    @Transactional(readOnly = true)
    public WalletBucket getBucket(UUID walletId, String currency) {
        return walletBucketRepository.findByWalletIdAndCurrency(walletId, currency)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "BUCKET_NOT_FOUND", "Currency bucket not found"));
    }
}