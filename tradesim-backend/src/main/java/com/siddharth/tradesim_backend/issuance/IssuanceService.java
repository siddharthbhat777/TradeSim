package com.siddharth.tradesim_backend.issuance;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.company.enums.CompanyStatus;
import com.siddharth.tradesim_backend.company.model.Company;
import com.siddharth.tradesim_backend.company.repository.CompanyRepository;
import com.siddharth.tradesim_backend.company.service.CompanyRepresentativeAssignmentService;
import com.siddharth.tradesim_backend.issuance.enums.IssuanceStatus;
import com.siddharth.tradesim_backend.issuance.model.IssuanceRequest;
import com.siddharth.tradesim_backend.issuance.model.dto.CreateIssuanceRequest;
import com.siddharth.tradesim_backend.issuance.model.dto.IssuanceRequestResponse;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IssuanceService {
    private final IssuanceRequestRepository issuanceRequestRepository;
    private final CompanyRepository companyRepository;
    private final StockRepository stockRepository;
    private final AuthRepository authRepository;
    private final PositionRepository positionRepository;
    private final CompanyRepresentativeAssignmentService companyRepresentativeAssignmentService;
    private final StockService stockService;

    @Transactional
    public IssuanceRequestResponse submitIssuanceRequest(UUID companyId, UUID stockId, UUID actingUserId, CreateIssuanceRequest request) {
        Company company = companyRepository.findById(companyId).orElseThrow(() -> new BusinessException("Company not found"));
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new BusinessException("Company is not active");
        }

        companyRepresentativeAssignmentService.assertPrimaryContactAssignment(companyId, actingUserId);

        validateStockForIssuanceSubmission(companyId, stockId);
        validateShareSplit(request.totalIssuedShares(), request.tradableFloatShares());

        User liquidityProvider = authRepository.findById(request.liquidityProviderUserId()).orElseThrow(() -> new BusinessException("Liquidity provider user not found"));
        assertLiquidityProvider(liquidityProvider);

        if (issuanceRequestRepository.existsByStockIdAndStatus(stockId, IssuanceStatus.PENDING)) {
            throw new BusinessException("A pending issuance request already exists for this stock");
        }

        if (issuanceRequestRepository.existsByStockIdAndStatus(stockId, IssuanceStatus.APPROVED)) {
            throw new BusinessException("Initial issuance has already been approved for this stock");
        }

        IssuanceRequest issuanceRequest = IssuanceRequest.builder()
                .companyId(companyId)
                .stockId(stockId)
                .submittedByUserId(actingUserId)
                .totalIssuedShares(request.totalIssuedShares())
                .tradableFloatShares(request.tradableFloatShares())
                .liquidityProviderUserId(request.liquidityProviderUserId())
                .status(IssuanceStatus.PENDING)
                .build();

        IssuanceRequest saved = issuanceRequestRepository.save(issuanceRequest);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<IssuanceRequestResponse> fetchPendingIssuanceRequests() {
        return issuanceRequestRepository.findByStatusOrderByCreatedAtAsc(IssuanceStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public IssuanceRequestResponse approveIssuanceRequest(UUID issuanceRequestId, UUID adminUserId) {
        IssuanceRequest issuanceRequest = findPendingIssuanceRequest(issuanceRequestId);

        Company company = companyRepository.findById(issuanceRequest.getCompanyId()).orElseThrow(() -> new BusinessException("Company not found"));
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new BusinessException("Company is not active");
        }

        Stock stock = stockRepository.findById(issuanceRequest.getStockId()).orElseThrow(() -> new BusinessException("Stock not found"));
        if (!stock.getCompanyId().equals(issuanceRequest.getCompanyId())) {
            throw new BusinessException("Stock does not belong to this company");
        }
        if (stock.getStatus() != StockStatus.HALTED) {
            throw new BusinessException("Only HALTED stocks can be activated through issuance");
        }
        if (stock.getTotalIssuedShares() != null || stock.getTradableFloatShares() != null) {
            throw new BusinessException("Initial issuance has already been applied to this stock");
        }

        validateShareSplit(issuanceRequest.getTotalIssuedShares(), issuanceRequest.getTradableFloatShares());

        User liquidityProvider = authRepository.findById(issuanceRequest.getLiquidityProviderUserId()).orElseThrow(() -> new BusinessException("Liquidity provider user not found"));
        assertLiquidityProvider(liquidityProvider);

        allocateTradableFloat(liquidityProvider.getId(), stock, issuanceRequest.getTradableFloatShares());
        stockService.activateStockFromIssuanceApproval(
                stock.getId(),
                issuanceRequest.getTotalIssuedShares(),
                issuanceRequest.getTradableFloatShares()
        );

        issuanceRequest.setStatus(IssuanceStatus.APPROVED);
        issuanceRequest.setReviewedByUserId(adminUserId);
        issuanceRequest.setReviewedAt(Instant.now());
        issuanceRequest.setRejectionReason(null);

        IssuanceRequest saved = issuanceRequestRepository.save(issuanceRequest);
        return toResponse(saved);
    }

    @Transactional
    public IssuanceRequestResponse rejectIssuanceRequest(UUID issuanceRequestId, String rejectionReason, UUID adminUserId) {
        IssuanceRequest issuanceRequest = findPendingIssuanceRequest(issuanceRequestId);

        issuanceRequest.setStatus(IssuanceStatus.REJECTED);
        issuanceRequest.setReviewedByUserId(adminUserId);
        issuanceRequest.setReviewedAt(Instant.now());
        issuanceRequest.setRejectionReason(rejectionReason);

        IssuanceRequest saved = issuanceRequestRepository.save(issuanceRequest);
        return toResponse(saved);
    }

    private void validateStockForIssuanceSubmission(UUID companyId, UUID stockId) {
        Stock stock = stockRepository.findById(stockId).orElseThrow(() -> new BusinessException("Stock not found"));

        if (!stock.getCompanyId().equals(companyId)) {
            throw new BusinessException("Stock does not belong to this company");
        }

        if (stock.getStatus() != StockStatus.HALTED) {
            throw new BusinessException("Issuance can only be requested for HALTED stocks");
        }

        if (stock.getTotalIssuedShares() != null || stock.getTradableFloatShares() != null) {
            throw new BusinessException("Initial issuance has already been applied to this stock");
        }
    }

    private void validateShareSplit(int totalIssuedShares, int tradableFloatShares) {
        if (tradableFloatShares > totalIssuedShares) {
            throw new BusinessException("Tradable float shares cannot exceed total issued shares");
        }
    }

    private void assertLiquidityProvider(User liquidityProvider) {
        if (liquidityProvider.getRole() != Role.USER || liquidityProvider.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Liquidity provider must be an active USER account");
        }
    }

    private void allocateTradableFloat(UUID liquidityProviderUserId, Stock stock, int tradableFloatShares) {
        Position position = positionRepository.findByUserIdAndStockId(liquidityProviderUserId, stock.getId())
                .orElse(Position.builder()
                        .userId(liquidityProviderUserId)
                        .stockId(stock.getId())
                        .quantity(0)
                        .lockedQuantity(0)
                        .averageBuyPrice(stock.getLastTradedPrice())
                        .realizedPnl(BigDecimal.ZERO)
                        .build());

        position.updateAverageBuyPrice(stock.getLastTradedPrice(), tradableFloatShares);
        position.increaseQuantity(tradableFloatShares);
        positionRepository.save(position);
    }

    private IssuanceRequest findPendingIssuanceRequest(UUID issuanceRequestId) {
        IssuanceRequest issuanceRequest = issuanceRequestRepository.findById(issuanceRequestId).orElseThrow(() -> new BusinessException("Issuance request not found"));

        if (issuanceRequest.getStatus() != IssuanceStatus.PENDING) {
            throw new BusinessException("Only pending issuance requests can be reviewed");
        }

        return issuanceRequest;
    }

    private IssuanceRequestResponse toResponse(IssuanceRequest issuanceRequest) {
        return new IssuanceRequestResponse(
                issuanceRequest.getId(),
                issuanceRequest.getCompanyId(),
                issuanceRequest.getStockId(),
                issuanceRequest.getSubmittedByUserId(),
                issuanceRequest.getTotalIssuedShares(),
                issuanceRequest.getTradableFloatShares(),
                issuanceRequest.getLiquidityProviderUserId(),
                issuanceRequest.getStatus(),
                issuanceRequest.getReviewedByUserId(),
                issuanceRequest.getReviewedAt(),
                issuanceRequest.getRejectionReason(),
                issuanceRequest.getCreatedAt(),
                issuanceRequest.getUpdatedAt()
        );
    }
}