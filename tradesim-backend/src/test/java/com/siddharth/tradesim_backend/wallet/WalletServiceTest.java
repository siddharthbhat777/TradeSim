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
import com.siddharth.tradesim_backend.wallet.model.dto.WalletResponse;
import com.siddharth.tradesim_backend.wallet.repository.WalletBucketRepository;
import com.siddharth.tradesim_backend.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    void shouldFetchPendingMultiCurrencyRequests() {
        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .multiCurrencyStatus(MultiCurrencyStatus.PENDING)
                .buckets(List.of())
                .build();

        when(walletRepository.findByMultiCurrencyStatusOrderByCreatedAtDesc(MultiCurrencyStatus.PENDING))
                .thenReturn(List.of(wallet));

        List<WalletResponse> responses = walletService.fetchPendingMultiCurrencyRequests();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().multiCurrencyStatus()).isEqualTo(MultiCurrencyStatus.PENDING);
        verify(walletRepository).findByMultiCurrencyStatusOrderByCreatedAtDesc(MultiCurrencyStatus.PENDING);
    }

    @Test
    void shouldCreateWalletForUserSuccessfully() {
        UUID userId = UUID.randomUUID();

        when(walletRepository.existsByUserId(userId)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet wallet = invocation.getArgument(0);
            wallet.setId(UUID.randomUUID());
            return wallet;
        });

        walletService.createWalletForUser(userId, "INR");

        verify(walletRepository).save(any(Wallet.class));
        verify(walletBucketRepository).save(any(WalletBucket.class));
    }

    @Test
    void shouldThrowWhenWalletAlreadyExistsForUser() {
        UUID userId = UUID.randomUUID();

        when(walletRepository.existsByUserId(userId)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> walletService.createWalletForUser(userId, "INR"));

        assertThat(exception.getMessage()).isEqualTo("Wallet already exists for this user");
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void shouldFetchMyWallet() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .multiCurrencyStatus(MultiCurrencyStatus.APPROVED)
                .buckets(List.of())
                .build();

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.fetchMyWallet(userId);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.multiCurrencyStatus()).isEqualTo(MultiCurrencyStatus.APPROVED);
    }

    @Test
    void shouldDepositFromBank() {
        UUID userId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(1000);

        User user = User.builder()
                .id(userId)
                .bankBalance(BigDecimal.valueOf(5000))
                .build();

        TradingAccount tradingAccount = TradingAccount.builder()
                .userId(userId)
                .baseCurrency("INR")
                .build();

        Wallet wallet = Wallet.builder().id(UUID.randomUUID()).userId(userId).buckets(List.of()).build();
        WalletBucket bucket = WalletBucket.builder().wallet(wallet).currency("INR").balance(BigDecimal.ZERO).lockedBalance(BigDecimal.ZERO).build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletBucketRepository.findByWalletIdAndCurrencyForUpdate(wallet.getId(), "INR")).thenReturn(Optional.of(bucket));

        walletService.depositFromBank(userId, amount);

        assertThat(user.getBankBalance()).isEqualByComparingTo(BigDecimal.valueOf(4000));
        assertThat(bucket.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000));

        verify(authRepository).save(user);
        verify(walletBucketRepository).save(bucket);
        verify(ledgerService).recordDeposit(bucket, tradingAccount, amount);
    }

    @Test
    void shouldThrowWhenBankBalanceInsufficientForDeposit() {
        UUID userId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(5000);

        User user = User.builder()
                .id(userId)
                .bankBalance(BigDecimal.valueOf(1000))
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));

        BusinessException exception = assertThrows(BusinessException.class, () -> walletService.depositFromBank(userId, amount));

        assertThat(exception.getMessage()).isEqualTo("Insufficient funds in simulated bank account");
    }

    @Test
    void shouldWithdrawToBank() {
        UUID userId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(1000);

        User user = User.builder()
                .id(userId)
                .bankBalance(BigDecimal.valueOf(1000))
                .build();

        TradingAccount tradingAccount = TradingAccount.builder()
                .userId(userId)
                .baseCurrency("INR")
                .marginLoan(BigDecimal.ZERO)
                .build();

        Wallet wallet = Wallet.builder().id(UUID.randomUUID()).userId(userId).buckets(List.of()).build();
        WalletBucket bucket = WalletBucket.builder().wallet(wallet).currency("INR").balance(BigDecimal.valueOf(5000)).lockedBalance(BigDecimal.ZERO).build();

        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletBucketRepository.findByWalletIdAndCurrencyForUpdate(wallet.getId(), "INR")).thenReturn(Optional.of(bucket));
        when(authRepository.findById(userId)).thenReturn(Optional.of(user));

        walletService.withdrawToBank(userId, amount);

        assertThat(user.getBankBalance()).isEqualByComparingTo(BigDecimal.valueOf(2000));
        assertThat(bucket.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(4000));

        verify(authRepository).save(user);
        verify(walletBucketRepository).save(bucket);
        verify(ledgerService).recordWithdrawal(bucket, tradingAccount, amount);
    }

    @Test
    void shouldThrowWhenActiveMarginLoanExistsOnWithdraw() {
        UUID userId = UUID.randomUUID();

        TradingAccount tradingAccount = TradingAccount.builder()
                .userId(userId)
                .baseCurrency("INR")
                .marginLoan(BigDecimal.valueOf(500))
                .build();

        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);

        BusinessException exception = assertThrows(BusinessException.class, () -> walletService.withdrawToBank(userId, BigDecimal.valueOf(1000)));

        assertThat(exception.getMessage()).isEqualTo("Cannot withdraw funds while you have an active margin loan");
    }

    @Test
    void shouldRequestMultiCurrencyAccess() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .multiCurrencyStatus(MultiCurrencyStatus.UNREQUESTED)
                .buckets(List.of())
                .build();

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse response = walletService.requestMultiCurrencyAccess(userId);

        assertThat(response.multiCurrencyStatus()).isEqualTo(MultiCurrencyStatus.PENDING);
        verify(walletRepository).save(wallet);
    }

    @Test
    void shouldConvertCurrency() {
        UUID userId = UUID.randomUUID();
        CurrencyConversionRequest request = new CurrencyConversionRequest("INR", "USD", BigDecimal.valueOf(10000));

        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .multiCurrencyStatus(MultiCurrencyStatus.APPROVED)
                .buckets(new java.util.ArrayList<>())
                .build();

        WalletBucket sourceBucket = WalletBucket.builder()
                .wallet(wallet)
                .currency("INR")
                .balance(BigDecimal.valueOf(50000))
                .lockedBalance(BigDecimal.ZERO)
                .build();
        wallet.getBuckets().add(sourceBucket);

        TradingAccount tradingAccount = TradingAccount.builder().userId(userId).build();

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletBucketRepository.findByWalletIdAndCurrencyForUpdate(wallet.getId(), "INR")).thenReturn(Optional.of(sourceBucket));
        when(walletBucketRepository.findByWalletIdAndCurrencyForUpdate(wallet.getId(), "USD")).thenReturn(Optional.empty());
        when(forexService.convert(request.amountToConvert(), "INR", "USD")).thenReturn(BigDecimal.valueOf(125));
        when(fxFeeService.calculateConversionFee("INR", "USD", BigDecimal.valueOf(125))).thenReturn(BigDecimal.valueOf(1));
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);

        walletService.convertCurrency(userId, request);

        assertThat(sourceBucket.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(40000));
        assertThat(wallet.getBuckets()).hasSize(2);
        WalletBucket targetBucket = wallet.getBuckets().stream().filter(b -> b.getCurrency().equals("USD")).findFirst().orElseThrow();
        assertThat(targetBucket.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(124));

        verify(walletBucketRepository, times(2)).save(any(WalletBucket.class));
        verify(ledgerService).recordFxConversionFee(targetBucket, tradingAccount, BigDecimal.valueOf(1), null, null, null, "INR", "USD");
    }

    @Test
    void shouldThrowWhenConvertingWithoutMultiCurrencyApproval() {
        UUID userId = UUID.randomUUID();
        CurrencyConversionRequest request = new CurrencyConversionRequest("INR", "USD", BigDecimal.valueOf(10000));

        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .multiCurrencyStatus(MultiCurrencyStatus.UNREQUESTED)
                .build();

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        BusinessException exception = assertThrows(BusinessException.class, () -> walletService.convertCurrency(userId, request));

        assertThat(exception.getMessage()).isEqualTo("You must be approved for Multi-Currency access to convert funds");
        verify(walletBucketRepository, never()).save(any(WalletBucket.class));
    }
}