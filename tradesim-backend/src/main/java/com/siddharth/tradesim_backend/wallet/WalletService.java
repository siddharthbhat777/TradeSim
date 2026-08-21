package com.siddharth.tradesim_backend.wallet;

import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.repository.AuthRepository;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.forex.service.ForexService;
import com.siddharth.tradesim_backend.forex.service.FxFeeService;
import com.siddharth.tradesim_backend.ledger.LedgerService;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import com.siddharth.tradesim_backend.wallet.enums.MultiCurrencyStatus;
import com.siddharth.tradesim_backend.wallet.model.Wallet;
import com.siddharth.tradesim_backend.wallet.model.WalletBucket;
import com.siddharth.tradesim_backend.wallet.model.dto.CurrencyConversionRequest;
import com.siddharth.tradesim_backend.wallet.model.dto.WalletBucketResponse;
import com.siddharth.tradesim_backend.wallet.model.dto.WalletResponse;
import com.siddharth.tradesim_backend.wallet.repository.WalletBucketRepository;
import com.siddharth.tradesim_backend.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;
    private final WalletBucketRepository walletBucketRepository;
    private final AuthRepository authRepository;
    private final TradingAccountService tradingAccountService;
    private final ForexService forexService;
    private final FxFeeService fxFeeService;
    private final LedgerService ledgerService;

    @Transactional
    public void createWalletForUser(UUID userId, String baseCurrency) {
        if (walletRepository.existsByUserId(userId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "WALLET_EXISTS", "Wallet already exists for this user");
        }

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .multiCurrencyStatus(MultiCurrencyStatus.UNREQUESTED)
                .build();

        Wallet savedWallet = walletRepository.save(wallet);

        WalletBucket baseBucket = WalletBucket.builder()
                .wallet(savedWallet)
                .currency(baseCurrency != null ? baseCurrency : "INR")
                .balance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .build();

        walletBucketRepository.save(baseBucket);
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

    @Transactional(readOnly = true)
    public WalletResponse fetchMyWallet(UUID userId) {
        Wallet wallet = getWalletByUserId(userId);
        return toResponse(wallet);
    }

    @Transactional
    public WalletResponse depositFromBank(UUID userId, BigDecimal amount) {
        User user = authRepository.findById(userId).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        if (user.getBankBalance().compareTo(amount) < 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "INSUFFICIENT_BANK_FUNDS", "Insufficient funds in simulated bank account");
        }

        TradingAccount tradingAccount = tradingAccountService.getTradingAccountByUserId(userId);
        Wallet wallet = getWalletByUserId(userId);
        WalletBucket baseBucket = getBucketForUpdate(wallet.getId(), tradingAccount.getBaseCurrency());

        user.setBankBalance(user.getBankBalance().subtract(amount));
        authRepository.save(user);

        baseBucket.setBalance(baseBucket.getBalance().add(amount));
        walletBucketRepository.save(baseBucket);

        ledgerService.recordDeposit(baseBucket, tradingAccount, amount);

        return fetchMyWallet(userId);
    }

    @Transactional
    public WalletResponse withdrawToBank(UUID userId, BigDecimal amount) {
        TradingAccount tradingAccount = tradingAccountService.getTradingAccountByUserId(userId);

        if (tradingAccount.getMarginLoan().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "ACTIVE_MARGIN_LOAN", "Cannot withdraw funds while you have an active margin loan");
        }

        Wallet wallet = getWalletByUserId(userId);
        WalletBucket baseBucket = getBucketForUpdate(wallet.getId(), tradingAccount.getBaseCurrency());

        if (baseBucket.getAvailableBalance().compareTo(amount) < 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "INSUFFICIENT_WALLET_FUNDS", "Insufficient available funds in base wallet");
        }

        User user = authRepository.findById(userId).orElseThrow();

        baseBucket.setBalance(baseBucket.getBalance().subtract(amount));
        walletBucketRepository.save(baseBucket);

        user.setBankBalance(user.getBankBalance().add(amount));
        authRepository.save(user);

        ledgerService.recordWithdrawal(baseBucket, tradingAccount, amount);

        return fetchMyWallet(userId);
    }

    @Transactional
    public WalletResponse requestMultiCurrencyAccess(UUID userId) {
        Wallet wallet = getWalletByUserId(userId);
        if (wallet.getMultiCurrencyStatus() == MultiCurrencyStatus.APPROVED) {
            throw new BusinessException(HttpStatus.CONFLICT, "ALREADY_APPROVED", "Multi-currency access is already approved");
        }
        if (wallet.getMultiCurrencyStatus() == MultiCurrencyStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "ALREADY_PENDING", "A request is already pending approval");
        }

        wallet.setMultiCurrencyStatus(MultiCurrencyStatus.PENDING);
        walletRepository.save(wallet);
        return toResponse(wallet);
    }

    @Transactional(readOnly = true)
    public List<WalletResponse> fetchPendingMultiCurrencyRequests() {
        return walletRepository.findByMultiCurrencyStatusOrderByCreatedAtAsc(MultiCurrencyStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public WalletResponse approveMultiCurrencyAccess(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        wallet.setMultiCurrencyStatus(MultiCurrencyStatus.APPROVED);
        return toResponse(walletRepository.save(wallet));
    }

    @Transactional
    public WalletResponse rejectMultiCurrencyAccess(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        wallet.setMultiCurrencyStatus(MultiCurrencyStatus.REJECTED);
        return toResponse(walletRepository.save(wallet));
    }

    @Transactional
    public WalletResponse convertCurrency(UUID userId, CurrencyConversionRequest request) {
        Wallet wallet = getWalletByUserId(userId);

        if (wallet.getMultiCurrencyStatus() != MultiCurrencyStatus.APPROVED) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "UNAUTHORIZED_CONVERSION", "You must be approved for Multi-Currency access to convert funds");
        }

        if (request.sourceCurrency().equals(request.targetCurrency())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_CONVERSION", "Source and target currencies must be different");
        }

        WalletBucket sourceBucket = getBucketForUpdate(wallet.getId(), request.sourceCurrency());

        if (sourceBucket.getAvailableBalance().compareTo(request.amount()) < 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS", "Insufficient available funds in source currency");
        }

        WalletBucket targetBucket = walletBucketRepository.findByWalletIdAndCurrencyForUpdate(wallet.getId(), request.targetCurrency())
                .orElseGet(() -> {
                    WalletBucket newBucket = WalletBucket.builder()
                            .wallet(wallet)
                            .currency(request.targetCurrency())
                            .balance(BigDecimal.ZERO)
                            .lockedBalance(BigDecimal.ZERO)
                            .build();
                    wallet.getBuckets().add(newBucket);
                    return newBucket;
                });

        BigDecimal convertedAmount = forexService.convert(request.amount(), request.sourceCurrency(), request.targetCurrency());
        BigDecimal fxFee = fxFeeService.calculateConversionFee(request.sourceCurrency(), request.targetCurrency(), convertedAmount);
        BigDecimal netAmount = convertedAmount.subtract(fxFee);

        sourceBucket.setBalance(sourceBucket.getBalance().subtract(request.amount()));
        walletBucketRepository.save(sourceBucket);

        targetBucket.setBalance(targetBucket.getBalance().add(netAmount));
        walletBucketRepository.save(targetBucket);

        if (fxFee.compareTo(BigDecimal.ZERO) > 0) {
            TradingAccount tradingAccount = tradingAccountService.getTradingAccountByUserId(userId);
            ledgerService.recordFxConversionFee(targetBucket, tradingAccount, fxFee, null, null, null, request.sourceCurrency(), request.targetCurrency());
        }

        return fetchMyWallet(userId);
    }

    private WalletResponse toResponse(Wallet wallet) {
        List<WalletBucketResponse> bucketResponses = wallet.getBuckets().stream()
                .map(bucket -> new WalletBucketResponse(
                        bucket.getId(),
                        bucket.getCurrency(),
                        bucket.getBalance(),
                        bucket.getLockedBalance(),
                        bucket.getAvailableBalance()
                )).toList();

        return new WalletResponse(wallet.getId(), wallet.getUserId(), wallet.getMultiCurrencyStatus(), bucketResponses);
    }
}