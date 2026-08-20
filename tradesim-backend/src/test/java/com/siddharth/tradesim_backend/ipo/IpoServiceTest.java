package com.siddharth.tradesim_backend.ipo;

import com.siddharth.tradesim_backend.auth.repository.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.company.enums.CompanyStatus;
import com.siddharth.tradesim_backend.company.model.Company;
import com.siddharth.tradesim_backend.company.repository.CompanyRepository;
import com.siddharth.tradesim_backend.company.service.CompanyRepresentativeAssignmentService;
import com.siddharth.tradesim_backend.exchange.ExchangeException;
import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.exchange.ExchangeService;
import com.siddharth.tradesim_backend.exchange.model.Exchange;
import com.siddharth.tradesim_backend.forex.service.ForexService;
import com.siddharth.tradesim_backend.forex.service.FxFeeService;
import com.siddharth.tradesim_backend.ipo.enums.IpoOfferStatus;
import com.siddharth.tradesim_backend.ipo.enums.IpoSubscriptionStatus;
import com.siddharth.tradesim_backend.ipo.model.IpoOffer;
import com.siddharth.tradesim_backend.ipo.model.IpoSubscription;
import com.siddharth.tradesim_backend.ipo.model.dto.CreateIpoOfferRequest;
import com.siddharth.tradesim_backend.ipo.model.dto.IpoOfferResponse;
import com.siddharth.tradesim_backend.ipo.model.dto.IpoSubscriptionResponse;
import com.siddharth.tradesim_backend.ipo.repository.IpoOfferRepository;
import com.siddharth.tradesim_backend.ipo.repository.IpoSubscriptionRepository;
import com.siddharth.tradesim_backend.ledger.LedgerService;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.enums.Sector;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.stock.model.dto.StockResponse;
import com.siddharth.tradesim_backend.stock.service.StockService;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import com.siddharth.tradesim_backend.wallet.model.Wallet;
import com.siddharth.tradesim_backend.wallet.model.WalletBucket;
import com.siddharth.tradesim_backend.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IpoServiceTest {

    @Mock
    private IpoOfferRepository ipoOfferRepository;

    @Mock
    private IpoSubscriptionRepository ipoSubscriptionRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private AuthRepository authRepository;

    @Mock
    private CompanyRepresentativeAssignmentService companyRepresentativeAssignmentService;

    @Mock
    private ExchangeService exchangeService;

    @Mock
    private TradingAccountService tradingAccountService;

    @Mock
    private WalletService walletService;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private StockService stockService;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private ExchangeRepository exchangeRepository;

    @Mock
    private ForexService forexService;

    @Mock
    private FxFeeService fxFeeService;

    @InjectMocks
    private IpoService ipoService;

    @Test
    void shouldSubmitIpoOfferWhenPrimaryContactAndStockAreValid() {
        UUID companyId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        UUID primaryContactUserId = UUID.randomUUID();

        CreateIpoOfferRequest request = new CreateIpoOfferRequest(
                BigDecimal.valueOf(125.50),
                100,
                5,
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(600)
        );

        Company company = Company.builder()
                .id(companyId)
                .status(CompanyStatus.ACTIVE)
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .companyId(companyId)
                .exchangeId(UUID.randomUUID())
                .status(StockStatus.HALTED)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(ipoOfferRepository.existsByStockIdAndStatusIn(eq(stockId), any())).thenReturn(false);
        when(ipoOfferRepository.save(any(IpoOffer.class))).thenAnswer(invocation -> {
            IpoOffer ipoOffer = invocation.getArgument(0);
            ipoOffer.setId(UUID.randomUUID());
            return ipoOffer;
        });

        IpoOfferResponse response = ipoService.submitIpoOffer(companyId, stockId, primaryContactUserId, request);

        assertThat(response.stockId()).isEqualTo(stockId);
        assertThat(response.status()).isEqualTo(IpoOfferStatus.PENDING_APPROVAL);
        assertThat(response.totalSharesOffered()).isEqualTo(500);
        verify(companyRepresentativeAssignmentService).assertPrimaryContactAssignment(
                companyId,
                primaryContactUserId,
                "Only an active primary contact can submit IPO offers"
        );
        verify(exchangeService).assertExchangeActive(stock.getExchangeId());
    }

    @Test
    void shouldApprovePendingIpoOffer() {
        UUID ipoOfferId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        IpoOffer ipoOffer = IpoOffer.builder()
                .id(ipoOfferId)
                .companyId(companyId)
                .stockId(stockId)
                .submittedByUserId(UUID.randomUUID())
                .issuePrice(BigDecimal.valueOf(125.50))
                .sharesPerAllottee(100)
                .maxAllottees(5)
                .subscriptionStartAt(Instant.now().minusSeconds(60))
                .subscriptionEndAt(Instant.now().plusSeconds(600))
                .status(IpoOfferStatus.PENDING_APPROVAL)
                .build();

        Company company = Company.builder()
                .id(companyId)
                .status(CompanyStatus.ACTIVE)
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .companyId(companyId)
                .exchangeId(UUID.randomUUID())
                .status(StockStatus.HALTED)
                .build();

        when(ipoOfferRepository.findById(ipoOfferId)).thenReturn(Optional.of(ipoOffer));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(ipoOfferRepository.save(any(IpoOffer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IpoOfferResponse response = ipoService.approveIpoOffer(ipoOfferId, adminUserId);

        assertThat(response.status()).isEqualTo(IpoOfferStatus.SUBSCRIPTION_OPEN);
        assertThat(response.reviewedByUserId()).isEqualTo(adminUserId);
    }

    @Test
    void shouldLockFundsWhenUserSubscribesToOpenIpo() {
        UUID ipoOfferId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        IpoOffer ipoOffer = IpoOffer.builder()
                .id(ipoOfferId)
                .companyId(UUID.randomUUID())
                .stockId(stockId)
                .submittedByUserId(UUID.randomUUID())
                .issuePrice(BigDecimal.valueOf(100))
                .sharesPerAllottee(50)
                .maxAllottees(5)
                .subscriptionStartAt(Instant.now().minusSeconds(60))
                .subscriptionEndAt(Instant.now().plusSeconds(600))
                .status(IpoOfferStatus.SUBSCRIPTION_OPEN)
                .build();

        User user = User.builder()
                .id(userId)
                .role(Role.USER)
                .countryCode("US")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        TradingAccount tradingAccount = TradingAccount.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .baseCurrency("USD")
                .marginLoan(BigDecimal.ZERO)
                .leverage(5)
                .maintenanceMarginPercent(BigDecimal.valueOf(25))
                .build();

        Wallet wallet = Wallet.builder().id(UUID.randomUUID()).build();
        WalletBucket bucket = WalletBucket.builder().balance(BigDecimal.valueOf(100000)).lockedBalance(BigDecimal.ZERO).build();

        Stock stock = Stock.builder().id(stockId).exchangeId(UUID.randomUUID()).build();
        Exchange exchange = Exchange.builder().currency("USD").build();

        when(ipoOfferRepository.findById(ipoOfferId)).thenReturn(Optional.of(ipoOffer));
        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(ipoSubscriptionRepository.existsByIpoOfferIdAndUserId(ipoOfferId, userId)).thenReturn(false);
        when(tradingAccountService.getTradingAccountByUserIdForUpdate(userId)).thenReturn(tradingAccount);

        when(walletService.getWalletByUserId(userId)).thenReturn(wallet);
        when(walletService.getBucketForUpdate(wallet.getId(), "USD")).thenReturn(bucket);

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(exchangeRepository.findById(stock.getExchangeId())).thenReturn(Optional.of(exchange));
        when(forexService.convert(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fxFeeService.calculateConversionFee(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(ipoSubscriptionRepository.save(any(IpoSubscription.class))).thenAnswer(invocation -> {
            IpoSubscription subscription = invocation.getArgument(0);
            subscription.setId(UUID.randomUUID());
            return subscription;
        });

        IpoSubscriptionResponse response = ipoService.subscribeToIpo(ipoOfferId, userId);

        assertThat(response.status()).isEqualTo(IpoSubscriptionStatus.SUBMITTED);
        assertThat(response.lockedAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(bucket.getLockedBalance()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        verify(ledgerService).recordIpoSubscriptionLock(bucket, tradingAccount, BigDecimal.valueOf(5000), stockId, ipoOfferId);
    }

    @Test
    void shouldFinalizeIpoOfferAndActivateStockWhenEnoughSubscriptionsExist() {
        UUID ipoOfferId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        UUID userOneId = UUID.randomUUID();
        UUID userTwoId = UUID.randomUUID();

        IpoOffer ipoOffer = IpoOffer.builder()
                .id(ipoOfferId)
                .companyId(companyId)
                .stockId(stockId)
                .submittedByUserId(UUID.randomUUID())
                .issuePrice(BigDecimal.valueOf(100))
                .sharesPerAllottee(50)
                .maxAllottees(2)
                .subscriptionStartAt(Instant.now().minusSeconds(600))
                .subscriptionEndAt(Instant.now().minusSeconds(60))
                .status(IpoOfferStatus.SUBSCRIPTION_OPEN)
                .build();

        Company company = Company.builder()
                .id(companyId)
                .status(CompanyStatus.ACTIVE)
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .companyId(companyId)
                .exchangeId(UUID.randomUUID())
                .symbol("TS_MOTORS")
                .lastTradedPrice(BigDecimal.valueOf(250.50))
                .sector(Sector.INDUSTRIALS)
                .status(StockStatus.HALTED)
                .build();

        IpoSubscription subscriptionOne = IpoSubscription.builder()
                .id(UUID.randomUUID())
                .ipoOfferId(ipoOfferId)
                .userId(userOneId)
                .lockedAmount(BigDecimal.valueOf(5000))
                .allottedShares(0)
                .status(IpoSubscriptionStatus.SUBMITTED)
                .build();

        IpoSubscription subscriptionTwo = IpoSubscription.builder()
                .id(UUID.randomUUID())
                .ipoOfferId(ipoOfferId)
                .userId(userTwoId)
                .lockedAmount(BigDecimal.valueOf(5000))
                .allottedShares(0)
                .status(IpoSubscriptionStatus.SUBMITTED)
                .build();

        TradingAccount tradingAccountOne = TradingAccount.builder()
                .id(UUID.randomUUID())
                .userId(userOneId)
                .baseCurrency("INR")
                .marginLoan(BigDecimal.ZERO)
                .leverage(5)
                .build();

        TradingAccount tradingAccountTwo = TradingAccount.builder()
                .id(UUID.randomUUID())
                .userId(userTwoId)
                .baseCurrency("INR")
                .marginLoan(BigDecimal.ZERO)
                .leverage(5)
                .build();

        User user1 = User.builder().id(userOneId).countryCode("US").build();
        User user2 = User.builder().id(userTwoId).countryCode("US").build();

        Wallet wallet1 = Wallet.builder().id(UUID.randomUUID()).build();
        WalletBucket bucket1 = WalletBucket.builder().balance(BigDecimal.valueOf(10000)).lockedBalance(BigDecimal.valueOf(5000)).build();

        Wallet wallet2 = Wallet.builder().id(UUID.randomUUID()).build();
        WalletBucket bucket2 = WalletBucket.builder().balance(BigDecimal.valueOf(12000)).lockedBalance(BigDecimal.valueOf(5000)).build();

        Exchange exchange = Exchange.builder().currency("USD").build();

        StockResponse activatedStock = new StockResponse(
                stockId,
                "TS_MOTORS",
                "TradeSim Motors Limited",
                BigDecimal.valueOf(250.50),
                Sector.INDUSTRIALS,
                StockStatus.ACTIVE
        );

        when(ipoOfferRepository.findById(ipoOfferId)).thenReturn(Optional.of(ipoOffer));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(exchangeRepository.findById(stock.getExchangeId())).thenReturn(Optional.of(exchange));
        when(ipoSubscriptionRepository.findByIpoOfferIdOrderByCreatedAtAsc(ipoOfferId)).thenReturn(List.of(subscriptionOne, subscriptionTwo));

        when(tradingAccountService.getTradingAccountByUserIdForUpdate(userOneId)).thenReturn(tradingAccountOne);
        when(tradingAccountService.getTradingAccountByUserIdForUpdate(userTwoId)).thenReturn(tradingAccountTwo);

        when(authRepository.findById(userOneId)).thenReturn(Optional.of(user1));
        when(authRepository.findById(userTwoId)).thenReturn(Optional.of(user2));

        when(walletService.getWalletByUserId(userOneId)).thenReturn(wallet1);
        when(walletService.getWalletByUserId(userTwoId)).thenReturn(wallet2);
        when(walletService.getBucketForUpdate(wallet1.getId(), "INR")).thenReturn(bucket1);
        when(walletService.getBucketForUpdate(wallet2.getId(), "INR")).thenReturn(bucket2);

        when(positionRepository.findByUserIdAndStockId(userOneId, stockId)).thenReturn(Optional.empty());
        when(positionRepository.findByUserIdAndStockId(userTwoId, stockId)).thenReturn(Optional.empty());
        when(forexService.convert(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fxFeeService.calculateConversionFee(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(stockService.activateStockFromIpoAllotment(stockId, 100, 100)).thenReturn(activatedStock);
        when(ipoOfferRepository.save(any(IpoOffer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IpoOfferResponse response = ipoService.finalizeIpoOffer(ipoOfferId, adminUserId);

        assertThat(response.status()).isEqualTo(IpoOfferStatus.ALLOTTED);
        assertThat(response.finalizedByUserId()).isEqualTo(adminUserId);
        assertThat(bucket1.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(bucket1.getLockedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(bucket2.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(7000));
        assertThat(bucket2.getLockedBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        ArgumentCaptor<Position> positionCaptor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository, times(2)).save(positionCaptor.capture());

        List<Position> savedPositions = positionCaptor.getAllValues();
        assertThat(savedPositions).hasSize(2);
        assertThat(savedPositions.get(0).getQuantity()).isEqualTo(50);
        assertThat(savedPositions.get(1).getQuantity()).isEqualTo(50);

        verify(ledgerService, times(2)).recordIpoAllotmentDebit(any(WalletBucket.class), any(TradingAccount.class), eq(BigDecimal.valueOf(5000)), eq(stockId), eq(ipoOfferId));
        verify(stockService).activateStockFromIpoAllotment(stockId, 100, 100);
        verify(ipoSubscriptionRepository).saveAll(anyList());
    }

    @Test
    void shouldRejectFinalizationWhenSubscriptionCountIsLessThanConfiguredMaxAllottees() {
        UUID ipoOfferId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        IpoOffer ipoOffer = IpoOffer.builder()
                .id(ipoOfferId)
                .companyId(companyId)
                .stockId(stockId)
                .submittedByUserId(UUID.randomUUID())
                .issuePrice(BigDecimal.valueOf(100))
                .sharesPerAllottee(50)
                .maxAllottees(3)
                .subscriptionStartAt(Instant.now().minusSeconds(600))
                .subscriptionEndAt(Instant.now().minusSeconds(60))
                .status(IpoOfferStatus.SUBSCRIPTION_OPEN)
                .build();

        Company company = Company.builder()
                .id(companyId)
                .status(CompanyStatus.ACTIVE)
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .companyId(companyId)
                .exchangeId(UUID.randomUUID())
                .status(StockStatus.HALTED)
                .build();

        Exchange exchange = Exchange.builder().currency("USD").build();

        when(ipoOfferRepository.findById(ipoOfferId)).thenReturn(Optional.of(ipoOffer));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(exchangeRepository.findById(stock.getExchangeId())).thenReturn(Optional.of(exchange));
        when(ipoSubscriptionRepository.findByIpoOfferIdOrderByCreatedAtAsc(ipoOfferId)).thenReturn(List.of(
                IpoSubscription.builder().id(UUID.randomUUID()).ipoOfferId(ipoOfferId).userId(UUID.randomUUID()).lockedAmount(BigDecimal.valueOf(5000)).allottedShares(0).status(IpoSubscriptionStatus.SUBMITTED).build(),
                IpoSubscription.builder().id(UUID.randomUUID()).ipoOfferId(ipoOfferId).userId(UUID.randomUUID()).lockedAmount(BigDecimal.valueOf(5000)).allottedShares(0).status(IpoSubscriptionStatus.SUBMITTED).build()
        ));

        BusinessException exception = assertThrows(BusinessException.class, () -> ipoService.finalizeIpoOffer(ipoOfferId, UUID.randomUUID()));

        assertThat(exception.getMessage()).isEqualTo("Not enough subscriptions to finalize this IPO offer");
        verify(stockService, never()).activateStockFromIpoAllotment(any(), anyInt(), anyInt());
    }

    @Test
    void shouldRejectIpoSubmissionWhenExchangeIsInactive() {
        UUID companyId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        UUID primaryContactUserId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();

        CreateIpoOfferRequest request = new CreateIpoOfferRequest(
                BigDecimal.valueOf(125.50),
                100,
                5,
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(600)
        );

        Company company = Company.builder()
                .id(companyId)
                .status(CompanyStatus.ACTIVE)
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .companyId(companyId)
                .exchangeId(exchangeId)
                .status(StockStatus.HALTED)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        doThrow(ExchangeException.conflict("Exchange is not active")).when(exchangeService).assertExchangeActive(exchangeId);

        BusinessException exception = assertThrows(BusinessException.class, () -> ipoService.submitIpoOffer(companyId, stockId, primaryContactUserId, request));

        assertThat(exception.getMessage()).isEqualTo("Exchange is not active");
    }
}