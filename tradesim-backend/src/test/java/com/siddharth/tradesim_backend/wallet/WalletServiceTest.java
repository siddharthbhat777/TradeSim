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
import com.siddharth.tradesim_backend.wallet.repository.WalletBucketRepository;
import com.siddharth.tradesim_backend.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletBucketRepository walletBucketRepository;

    @Mock
    private AuthRepository authRepository;

    @Mock
    private TradingAccountService tradingAccountService;

    @Mock
    private ForexService forexService;

    @Mock
    private FxFeeService fxFeeService;

    @Mock
    private LedgerService ledgerService;

    @InjectMocks
    private WalletService walletService;

    @Test
    void shouldCreateWalletForUser() {
        UUID userId = UUID.randomUUID();

        when(walletRepository.existsByUserId(userId)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));

        walletService.createWalletForUser(userId, "INR");

        verify(walletRepository).save(any(Wallet.class));
        verify(walletBucketRepository).save(any(WalletBucket.class));
    }

    @Test
    void shouldDepositFromBank() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).bankBalance(BigDecimal.valueOf(5000)).build();
        TradingAccount account = TradingAccount.builder().baseCurrency("INR").build();
        Wallet wallet = Wallet.builder().id(UUID.randomUUID()).userId(userId).buckets(new ArrayList<>()).build();
        WalletBucket bucket = WalletBucket.builder().currency("INR").balance(BigDecimal.ZERO).lockedBalance(BigDecimal.ZERO).build();
        wallet.getBuckets().add(bucket);

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(account);
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletBucketRepository.findByWalletIdAndCurrencyForUpdate(wallet.getId(), "INR")).thenReturn(Optional.of(bucket));

        walletService.depositFromBank(userId, BigDecimal.valueOf(1000));

        assertThat(user.getBankBalance()).isEqualByComparingTo(BigDecimal.valueOf(4000));
        assertThat(bucket.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        verify(ledgerService).recordDeposit(eq(bucket), eq(account), eq(BigDecimal.valueOf(1000)));
    }

    @Test
    void shouldRejectConvertCurrencyIfNotApproved() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = Wallet.builder().multiCurrencyStatus(MultiCurrencyStatus.UNREQUESTED).build();
        CurrencyConversionRequest request = new CurrencyConversionRequest("INR", "USD", BigDecimal.valueOf(100));

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        BusinessException exception = assertThrows(BusinessException.class, () -> walletService.convertCurrency(userId, request));
        assertThat(exception.getErrorCode()).isEqualTo("UNAUTHORIZED_CONVERSION");
    }

    @Test
    void shouldConvertCurrencySuccessfully() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = Wallet.builder().id(UUID.randomUUID()).multiCurrencyStatus(MultiCurrencyStatus.APPROVED).buckets(new ArrayList<>()).build();
        WalletBucket inrBucket = WalletBucket.builder().wallet(wallet).currency("INR").balance(BigDecimal.valueOf(10000)).lockedBalance(BigDecimal.ZERO).build();
        WalletBucket usdBucket = WalletBucket.builder().wallet(wallet).currency("USD").balance(BigDecimal.ZERO).lockedBalance(BigDecimal.ZERO).build();
        wallet.getBuckets().add(inrBucket);
        wallet.getBuckets().add(usdBucket);

        CurrencyConversionRequest request = new CurrencyConversionRequest("INR", "USD", BigDecimal.valueOf(8000));

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletBucketRepository.findByWalletIdAndCurrencyForUpdate(wallet.getId(), "INR")).thenReturn(Optional.of(inrBucket));
        when(walletBucketRepository.findByWalletIdAndCurrencyForUpdate(wallet.getId(), "USD")).thenReturn(Optional.of(usdBucket));
        when(forexService.convert(BigDecimal.valueOf(8000), "INR", "USD")).thenReturn(BigDecimal.valueOf(100));
        when(fxFeeService.calculateConversionFee("INR", "USD", BigDecimal.valueOf(100))).thenReturn(BigDecimal.valueOf(2));
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(TradingAccount.builder().build());

        walletService.convertCurrency(userId, request);

        assertThat(inrBucket.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(2000));
        assertThat(usdBucket.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(98));
        verify(ledgerService).recordFxConversionFee(eq(usdBucket), any(), eq(BigDecimal.valueOf(2)), any(), any(), any(), eq("INR"), eq("USD"));
    }
}