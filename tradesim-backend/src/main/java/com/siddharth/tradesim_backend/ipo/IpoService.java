package com.siddharth.tradesim_backend.ipo;

import com.siddharth.tradesim_backend.auth.repository.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.company.CompanyException;
import com.siddharth.tradesim_backend.company.enums.CompanyStatus;
import com.siddharth.tradesim_backend.company.model.Company;
import com.siddharth.tradesim_backend.company.repository.CompanyRepository;
import com.siddharth.tradesim_backend.company.service.CompanyRepresentativeAssignmentService;
import com.siddharth.tradesim_backend.exchange.ExchangeException;
import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.exchange.ExchangeService;
import com.siddharth.tradesim_backend.exchange.model.Exchange;
import com.siddharth.tradesim_backend.forex.service.ForexService;
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
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.StockException;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.stock.service.StockService;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import com.siddharth.tradesim_backend.user.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IpoService {
    private final IpoOfferRepository ipoOfferRepository;
    private final IpoSubscriptionRepository ipoSubscriptionRepository;
    private final CompanyRepository companyRepository;
    private final StockRepository stockRepository;
    private final AuthRepository authRepository;
    private final CompanyRepresentativeAssignmentService companyRepresentativeAssignmentService;
    private final ExchangeService exchangeService;
    private final TradingAccountService tradingAccountService;
    private final PositionRepository positionRepository;
    private final StockService stockService;
    private final LedgerService ledgerService;
    private final ExchangeRepository exchangeRepository;
    private final ForexService forexService;

    @Transactional
    public IpoOfferResponse submitIpoOffer(UUID companyId, UUID stockId, UUID actingUserId, CreateIpoOfferRequest request) {
        Company company = companyRepository.findById(companyId).orElseThrow(() -> CompanyException.notFound("Company not found"));
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw CompanyException.conflict("Company is not active");
        }

        companyRepresentativeAssignmentService.assertPrimaryContactAssignment(companyId, actingUserId, "Only an active primary contact can submit IPO offers");

        validateOfferWindow(request.subscriptionStartAt(), request.subscriptionEndAt());
        validateStockForIpo(companyId, stockId);

        if (ipoOfferRepository.existsByStockIdAndStatusIn(stockId, List.of(IpoOfferStatus.PENDING_APPROVAL, IpoOfferStatus.SUBSCRIPTION_OPEN, IpoOfferStatus.ALLOTTED))) {
            throw IpoException.conflict("An IPO offer already exists for this stock");
        }

        IpoOffer ipoOffer = IpoOffer.builder()
                .companyId(companyId)
                .stockId(stockId)
                .submittedByUserId(actingUserId)
                .issuePrice(request.issuePrice())
                .sharesPerAllottee(request.sharesPerAllottee())
                .maxAllottees(request.maxAllottees())
                .subscriptionStartAt(request.subscriptionStartAt())
                .subscriptionEndAt(request.subscriptionEndAt())
                .status(IpoOfferStatus.PENDING_APPROVAL)
                .build();

        IpoOffer saved = ipoOfferRepository.save(ipoOffer);
        return toOfferResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<IpoOfferResponse> fetchPendingIpoOffers() {
        return ipoOfferRepository.findByStatusOrderByCreatedAtAsc(IpoOfferStatus.PENDING_APPROVAL).stream().map(this::toOfferResponse).toList();
    }

    @Transactional
    public IpoOfferResponse approveIpoOffer(UUID ipoOfferId, UUID adminUserId) {
        IpoOffer ipoOffer = findPendingIpoOffer(ipoOfferId);

        Company company = companyRepository.findById(ipoOffer.getCompanyId()).orElseThrow(() -> CompanyException.notFound("Company not found"));
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw CompanyException.conflict("Company is not active");
        }

        validateOfferWindow(ipoOffer.getSubscriptionStartAt(), ipoOffer.getSubscriptionEndAt());
        validateStockForIpo(ipoOffer.getCompanyId(), ipoOffer.getStockId());

        ipoOffer.setStatus(IpoOfferStatus.SUBSCRIPTION_OPEN);
        ipoOffer.setReviewedByUserId(adminUserId);
        ipoOffer.setReviewedAt(Instant.now());
        ipoOffer.setRejectionReason(null);

        IpoOffer saved = ipoOfferRepository.save(ipoOffer);
        return toOfferResponse(saved);
    }

    @Transactional
    public IpoOfferResponse rejectIpoOffer(UUID ipoOfferId, String rejectionReason, UUID adminUserId) {
        IpoOffer ipoOffer = findPendingIpoOffer(ipoOfferId);

        ipoOffer.setStatus(IpoOfferStatus.REJECTED);
        ipoOffer.setReviewedByUserId(adminUserId);
        ipoOffer.setReviewedAt(Instant.now());
        ipoOffer.setRejectionReason(rejectionReason);

        IpoOffer saved = ipoOfferRepository.save(ipoOffer);
        return toOfferResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<IpoOfferResponse> fetchOpenIpoOffers() {
        Instant now = Instant.now();

        return ipoOfferRepository.findByStatusOrderByCreatedAtAsc(IpoOfferStatus.SUBSCRIPTION_OPEN)
                .stream()
                .filter(offer -> !now.isBefore(offer.getSubscriptionStartAt()) && now.isBefore(offer.getSubscriptionEndAt()))
                .map(this::toOfferResponse)
                .toList();
    }

    @Transactional
    public IpoSubscriptionResponse subscribeToIpo(UUID ipoOfferId, UUID userId) {
        IpoOffer ipoOffer = ipoOfferRepository.findById(ipoOfferId).orElseThrow(() -> IpoException.notFound("IPO offer not found"));
        assertIpoOfferIsOpenForSubscription(ipoOffer);

        User user = authRepository.findById(userId).orElseThrow(() -> UserException.notFound("User not found"));
        assertActiveSubscriber(user);

        if (ipoSubscriptionRepository.existsByIpoOfferIdAndUserId(ipoOfferId, userId)) {
            throw IpoException.conflict("You have already subscribed to this IPO offer");
        }

        Stock stock = stockRepository.findById(ipoOffer.getStockId()).orElseThrow(() -> StockException.notFound("Stock not found"));
        Exchange exchange = exchangeRepository.findById(stock.getExchangeId()).orElseThrow(() -> ExchangeException.notFound("Exchange not found"));
        String stockCurrency = exchange.getCurrency();

        TradingAccount tradingAccount = tradingAccountService.getTradingAccountByUserIdForUpdate(userId);
        String userCurrency = tradingAccount.getBaseCurrency();

        BigDecimal subscriptionAmountInStockCurrency = calculateSubscriptionAmount(ipoOffer);
        BigDecimal subscriptionAmountInUserCurrency = forexService.convert(subscriptionAmountInStockCurrency, stockCurrency, userCurrency);

        tradingAccount.lockFunds(subscriptionAmountInUserCurrency);
        tradingAccountService.saveTradingAccount(tradingAccount);
        ledgerService.recordIpoSubscriptionLock(tradingAccount, subscriptionAmountInUserCurrency, ipoOffer.getStockId(), ipoOffer.getId());

        IpoSubscription ipoSubscription = IpoSubscription.builder()
                .ipoOfferId(ipoOfferId)
                .userId(userId)
                .lockedAmount(subscriptionAmountInUserCurrency)
                .allottedShares(0)
                .status(IpoSubscriptionStatus.SUBMITTED)
                .build();

        IpoSubscription saved = ipoSubscriptionRepository.save(ipoSubscription);
        return toSubscriptionResponse(saved, ipoOffer);
    }

    @Transactional(readOnly = true)
    public List<IpoSubscriptionResponse> fetchMySubscriptions(UUID userId) {
        List<IpoSubscription> subscriptions = ipoSubscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<UUID, IpoOffer> offerMap = new HashMap<>();

        for (IpoOffer offer : ipoOfferRepository.findAllById(subscriptions.stream().map(IpoSubscription::getIpoOfferId).toList())) {
            offerMap.put(offer.getId(), offer);
        }

        return subscriptions.stream()
                .map(subscription -> {
                    IpoOffer offer = offerMap.get(subscription.getIpoOfferId());
                    if (offer == null) {
                        throw IpoException.notFound("IPO offer not found");
                    }
                    return toSubscriptionResponse(subscription, offer);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IpoSubscriptionResponse> fetchSubscriptionsForOffer(UUID ipoOfferId) {
        IpoOffer ipoOffer = ipoOfferRepository.findById(ipoOfferId).orElseThrow(() -> IpoException.notFound("IPO offer not found"));

        return ipoSubscriptionRepository.findByIpoOfferIdOrderByCreatedAtAsc(ipoOfferId)
                .stream()
                .map(subscription -> toSubscriptionResponse(subscription, ipoOffer))
                .toList();
    }

    @Transactional
    public IpoOfferResponse finalizeIpoOffer(UUID ipoOfferId, UUID adminUserId) {
        IpoOffer ipoOffer = ipoOfferRepository.findById(ipoOfferId).orElseThrow(() -> IpoException.notFound("IPO offer not found"));

        if (ipoOffer.getStatus() != IpoOfferStatus.SUBSCRIPTION_OPEN) {
            throw IpoException.conflict("Only open IPO offers can be finalized");
        }

        if (Instant.now().isBefore(ipoOffer.getSubscriptionEndAt())) {
            throw IpoException.conflict("IPO subscription window is still open");
        }

        Company company = companyRepository.findById(ipoOffer.getCompanyId()).orElseThrow(() -> CompanyException.notFound("Company not found"));
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw CompanyException.conflict("Company is not active");
        }

        Stock stock = validateStockForIpo(ipoOffer.getCompanyId(), ipoOffer.getStockId());
        Exchange exchange = exchangeRepository.findById(stock.getExchangeId()).orElseThrow(() -> ExchangeException.notFound("Exchange not found"));
        String stockCurrency = exchange.getCurrency();

        List<IpoSubscription> subscriptions = ipoSubscriptionRepository.findByIpoOfferIdOrderByCreatedAtAsc(ipoOfferId);
        if (subscriptions.size() < ipoOffer.getMaxAllottees()) {
            throw IpoException.conflict("Not enough subscriptions to finalize this IPO offer");
        }

        List<IpoSubscription> shuffledSubscriptions = new ArrayList<>(subscriptions);
        shuffleSubscriptions(shuffledSubscriptions, ipoOffer.getId());

        List<IpoSubscription> winningSubscriptions = shuffledSubscriptions.subList(0, ipoOffer.getMaxAllottees());
        List<IpoSubscription> losingSubscriptions = shuffledSubscriptions.subList(ipoOffer.getMaxAllottees(), shuffledSubscriptions.size());

        Map<UUID, TradingAccount> lockedAccounts = lockTradingAccounts(shuffledSubscriptions.stream().map(IpoSubscription::getUserId).toList());

        for (IpoSubscription winningSubscription : winningSubscriptions) {
            TradingAccount tradingAccount = lockedAccounts.get(winningSubscription.getUserId());
            String userCurrency = tradingAccount.getBaseCurrency();

            BigDecimal issuePriceInUserCurrency = forexService.convert(ipoOffer.getIssuePrice(), stockCurrency, userCurrency);

            tradingAccount.debitLockedFunds(winningSubscription.getLockedAmount());
            tradingAccountService.saveTradingAccount(tradingAccount);
            ledgerService.recordIpoAllotmentDebit(tradingAccount, winningSubscription.getLockedAmount(), stock.getId(), ipoOffer.getId());

            allocateIpoPosition(
                    winningSubscription.getUserId(),
                    stock,
                    issuePriceInUserCurrency,
                    ipoOffer.getSharesPerAllottee()
            );

            winningSubscription.setAllottedShares(ipoOffer.getSharesPerAllottee());
            winningSubscription.setStatus(IpoSubscriptionStatus.ALLOTTED);
        }

        for (IpoSubscription losingSubscription : losingSubscriptions) {
            TradingAccount tradingAccount = lockedAccounts.get(losingSubscription.getUserId());
            tradingAccount.unlockFunds(losingSubscription.getLockedAmount());
            tradingAccountService.saveTradingAccount(tradingAccount);
            ledgerService.recordIpoSubscriptionUnlock(tradingAccount, losingSubscription.getLockedAmount(), stock.getId(), ipoOffer.getId());

            losingSubscription.setAllottedShares(0);
            losingSubscription.setStatus(IpoSubscriptionStatus.NOT_ALLOTTED);
        }

        ipoSubscriptionRepository.saveAll(shuffledSubscriptions);

        int totalSharesOffered = calculateTotalSharesOffered(ipoOffer);
        stockService.activateStockFromIpoAllotment(stock.getId(), totalSharesOffered, totalSharesOffered);

        ipoOffer.setStatus(IpoOfferStatus.ALLOTTED);
        ipoOffer.setFinalizedByUserId(adminUserId);
        ipoOffer.setFinalizedAt(Instant.now());

        IpoOffer saved = ipoOfferRepository.save(ipoOffer);
        return toOfferResponse(saved);
    }

    private void assertActiveSubscriber(User user) {
        if (user.getRole() != Role.USER || user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw IpoException.forbidden("Only an active USER account can subscribe to IPO offers");
        }
    }

    private void assertIpoOfferIsOpenForSubscription(IpoOffer ipoOffer) {
        if (ipoOffer.getStatus() != IpoOfferStatus.SUBSCRIPTION_OPEN) {
            throw IpoException.conflict("IPO offer is not open for subscription");
        }

        Instant now = Instant.now();
        if (now.isBefore(ipoOffer.getSubscriptionStartAt())) {
            throw IpoException.conflict("IPO subscription window has not started yet");
        }
        if (!now.isBefore(ipoOffer.getSubscriptionEndAt())) {
            throw IpoException.conflict("IPO subscription window has already closed");
        }
    }

    private void validateOfferWindow(Instant subscriptionStartAt, Instant subscriptionEndAt) {
        if (!subscriptionEndAt.isAfter(subscriptionStartAt)) {
            throw IpoException.badRequest("Subscription end time must be after subscription start time");
        }
        if (!subscriptionEndAt.isAfter(Instant.now())) {
            throw IpoException.badRequest("Subscription end time must be in the future");
        }
    }

    private Stock validateStockForIpo(UUID companyId, UUID stockId) {
        Stock stock = stockRepository.findById(stockId).orElseThrow(() -> StockException.notFound("Stock not found"));

        if (!stock.getCompanyId().equals(companyId)) {
            throw IpoException.conflict("Stock does not belong to this company");
        }

        if (stock.getStatus() != StockStatus.HALTED) {
            throw IpoException.conflict("IPO is only allowed for HALTED stocks");
        }

        exchangeService.assertExchangeActive(stock.getExchangeId());

        if (stock.getTotalIssuedShares() != null || stock.getTradableFloatShares() != null) {
            throw IpoException.conflict("Initial share allocation has already been applied to this stock");
        }

        return stock;
    }

    private BigDecimal calculateSubscriptionAmount(IpoOffer ipoOffer) {
        return ipoOffer.getIssuePrice().multiply(BigDecimal.valueOf(ipoOffer.getSharesPerAllottee()));
    }

    private int calculateTotalSharesOffered(IpoOffer ipoOffer) {
        return ipoOffer.getSharesPerAllottee() * ipoOffer.getMaxAllottees();
    }

    private Map<UUID, TradingAccount> lockTradingAccounts(List<UUID> userIds) {
        Map<UUID, TradingAccount> lockedAccounts = new HashMap<>();

        userIds.stream()
                .distinct()
                .sorted(UUID::compareTo)
                .forEach(userId -> lockedAccounts.put(userId, tradingAccountService.getTradingAccountByUserIdForUpdate(userId)));

        return lockedAccounts;
    }

    private void shuffleSubscriptions(List<IpoSubscription> subscriptions, UUID ipoOfferId) {
        long seed = ipoOfferId.getMostSignificantBits() ^ ipoOfferId.getLeastSignificantBits();
        Collections.shuffle(subscriptions, new Random(seed));
    }

    private void allocateIpoPosition(UUID userId, Stock stock, BigDecimal issuePrice, int shareQuantity) {
        Position position = positionRepository.findByUserIdAndStockId(userId, stock.getId())
                .orElse(Position.builder()
                        .userId(userId)
                        .stockId(stock.getId())
                        .quantity(0)
                        .lockedQuantity(0)
                        .averageBuyPrice(issuePrice)
                        .realizedPnl(BigDecimal.ZERO)
                        .build());

        position.updateAverageBuyPrice(issuePrice, shareQuantity);
        position.increaseQuantity(shareQuantity);
        positionRepository.save(position);
    }

    private IpoOffer findPendingIpoOffer(UUID ipoOfferId) {
        IpoOffer ipoOffer = ipoOfferRepository.findById(ipoOfferId).orElseThrow(() -> IpoException.notFound("IPO offer not found"));

        if (ipoOffer.getStatus() != IpoOfferStatus.PENDING_APPROVAL) {
            throw IpoException.conflict("Only pending IPO offers can be reviewed");
        }

        return ipoOffer;
    }

    private IpoOfferResponse toOfferResponse(IpoOffer ipoOffer) {
        return new IpoOfferResponse(
                ipoOffer.getId(),
                ipoOffer.getCompanyId(),
                ipoOffer.getStockId(),
                ipoOffer.getSubmittedByUserId(),
                ipoOffer.getIssuePrice(),
                ipoOffer.getSharesPerAllottee(),
                ipoOffer.getMaxAllottees(),
                calculateTotalSharesOffered(ipoOffer),
                ipoOffer.getSubscriptionStartAt(),
                ipoOffer.getSubscriptionEndAt(),
                ipoOffer.getStatus(),
                ipoOffer.getReviewedByUserId(),
                ipoOffer.getReviewedAt(),
                ipoOffer.getFinalizedByUserId(),
                ipoOffer.getFinalizedAt(),
                ipoOffer.getRejectionReason(),
                ipoOffer.getCreatedAt(),
                ipoOffer.getUpdatedAt()
        );
    }

    private IpoSubscriptionResponse toSubscriptionResponse(IpoSubscription ipoSubscription, IpoOffer ipoOffer) {
        return new IpoSubscriptionResponse(
                ipoSubscription.getId(),
                ipoSubscription.getIpoOfferId(),
                ipoOffer.getStockId(),
                ipoSubscription.getUserId(),
                ipoOffer.getIssuePrice(),
                ipoSubscription.getLockedAmount(),
                ipoSubscription.getAllottedShares(),
                ipoSubscription.getStatus(),
                ipoSubscription.getCreatedAt(),
                ipoSubscription.getUpdatedAt()
        );
    }
}