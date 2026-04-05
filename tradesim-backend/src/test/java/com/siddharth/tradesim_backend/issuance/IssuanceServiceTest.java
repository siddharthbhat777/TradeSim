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
import com.siddharth.tradesim_backend.stock.enums.Sector;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.stock.model.dto.StockResponse;
import com.siddharth.tradesim_backend.stock.service.StockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssuanceServiceTest {

    @Mock
    private IssuanceRequestRepository issuanceRequestRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private AuthRepository authRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private CompanyRepresentativeAssignmentService companyRepresentativeAssignmentService;

    @Mock
    private StockService stockService;

    @InjectMocks
    private IssuanceService issuanceService;

    @Test
    void shouldSubmitIssuanceRequestWhenPrimaryContactAndStockAreValid() {
        UUID companyId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        UUID primaryContactUserId = UUID.randomUUID();
        UUID liquidityProviderUserId = UUID.randomUUID();

        CreateIssuanceRequest request = new CreateIssuanceRequest(1_000_000, 200_000, liquidityProviderUserId);

        Company company = Company.builder()
                .id(companyId)
                .status(CompanyStatus.ACTIVE)
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .companyId(companyId)
                .symbol("TS_MOTORS")
                .status(StockStatus.HALTED)
                .lastTradedPrice(BigDecimal.valueOf(250.50))
                .sector(Sector.INDUSTRIALS)
                .build();

        User liquidityProvider = User.builder()
                .id(liquidityProviderUserId)
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(authRepository.findById(liquidityProviderUserId)).thenReturn(Optional.of(liquidityProvider));
        when(issuanceRequestRepository.existsByStockIdAndStatus(stockId, IssuanceStatus.PENDING)).thenReturn(false);
        when(issuanceRequestRepository.existsByStockIdAndStatus(stockId, IssuanceStatus.APPROVED)).thenReturn(false);
        when(issuanceRequestRepository.save(any(IssuanceRequest.class))).thenAnswer(invocation -> {
            IssuanceRequest issuanceRequest = invocation.getArgument(0);
            issuanceRequest.setId(UUID.randomUUID());
            return issuanceRequest;
        });

        IssuanceRequestResponse response = issuanceService.submitIssuanceRequest(companyId, stockId, primaryContactUserId, request);

        assertThat(response.stockId()).isEqualTo(stockId);
        assertThat(response.status()).isEqualTo(IssuanceStatus.PENDING);
        assertThat(response.totalIssuedShares()).isEqualTo(1_000_000);
        assertThat(response.tradableFloatShares()).isEqualTo(200_000);
        verify(companyRepresentativeAssignmentService).assertPrimaryContactAssignment(companyId, primaryContactUserId);
    }

    @Test
    void shouldRejectIssuanceSubmissionWhenRequesterIsNotPrimaryContact() {
        UUID companyId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        UUID managerUserId = UUID.randomUUID();
        UUID liquidityProviderUserId = UUID.randomUUID();

        CreateIssuanceRequest request = new CreateIssuanceRequest(1_000_000, 200_000, liquidityProviderUserId);

        Company company = Company.builder()
                .id(companyId)
                .status(CompanyStatus.ACTIVE)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        doThrow(new BusinessException("Only an active primary contact can submit issuance requests"))
                .when(companyRepresentativeAssignmentService)
                .assertPrimaryContactAssignment(companyId, managerUserId);

        BusinessException exception = assertThrows(BusinessException.class, () -> issuanceService.submitIssuanceRequest(companyId, stockId, managerUserId, request));

        assertThat(exception.getMessage()).isEqualTo("Only an active primary contact can submit issuance requests");
        verify(issuanceRequestRepository, never()).save(any());
    }

    @Test
    void shouldRejectIssuanceSubmissionWhenTradableFloatExceedsTotalSupply() {
        UUID companyId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        UUID primaryContactUserId = UUID.randomUUID();
        UUID liquidityProviderUserId = UUID.randomUUID();

        CreateIssuanceRequest request = new CreateIssuanceRequest(100_000, 200_000, liquidityProviderUserId);

        Company company = Company.builder()
                .id(companyId)
                .status(CompanyStatus.ACTIVE)
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .companyId(companyId)
                .status(StockStatus.HALTED)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        BusinessException exception = assertThrows(BusinessException.class, () -> issuanceService.submitIssuanceRequest(companyId, stockId, primaryContactUserId, request));

        assertThat(exception.getMessage()).isEqualTo("Tradable float shares cannot exceed total issued shares");
        verify(issuanceRequestRepository, never()).save(any());
    }

    @Test
    void shouldApproveIssuanceAndAllocateTradableFloat() {
        UUID issuanceRequestId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        UUID primaryContactUserId = UUID.randomUUID();
        UUID liquidityProviderUserId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        IssuanceRequest issuanceRequest = IssuanceRequest.builder()
                .id(issuanceRequestId)
                .companyId(companyId)
                .stockId(stockId)
                .submittedByUserId(primaryContactUserId)
                .totalIssuedShares(1_000_000)
                .tradableFloatShares(200_000)
                .liquidityProviderUserId(liquidityProviderUserId)
                .status(IssuanceStatus.PENDING)
                .build();

        Company company = Company.builder()
                .id(companyId)
                .status(CompanyStatus.ACTIVE)
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .companyId(companyId)
                .symbol("TS_MOTORS")
                .lastTradedPrice(BigDecimal.valueOf(250.50))
                .status(StockStatus.HALTED)
                .build();

        User liquidityProvider = User.builder()
                .id(liquidityProviderUserId)
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        StockResponse activatedStock = new StockResponse(
                stockId,
                "TS_MOTORS",
                "TradeSim Motors Limited",
                BigDecimal.valueOf(250.50),
                Sector.INDUSTRIALS,
                StockStatus.ACTIVE
        );

        when(issuanceRequestRepository.findById(issuanceRequestId)).thenReturn(Optional.of(issuanceRequest));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(authRepository.findById(liquidityProviderUserId)).thenReturn(Optional.of(liquidityProvider));
        when(positionRepository.findByUserIdAndStockId(liquidityProviderUserId, stockId)).thenReturn(Optional.empty());
        when(stockService.activateStockFromIssuanceApproval(stockId, 1_000_000, 200_000)).thenReturn(activatedStock);
        when(issuanceRequestRepository.save(any(IssuanceRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IssuanceRequestResponse response = issuanceService.approveIssuanceRequest(issuanceRequestId, adminUserId);

        assertThat(response.status()).isEqualTo(IssuanceStatus.APPROVED);
        assertThat(response.reviewedByUserId()).isEqualTo(adminUserId);

        ArgumentCaptor<Position> positionCaptor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(positionCaptor.capture());

        Position savedPosition = positionCaptor.getValue();
        assertThat(savedPosition.getUserId()).isEqualTo(liquidityProviderUserId);
        assertThat(savedPosition.getStockId()).isEqualTo(stockId);
        assertThat(savedPosition.getQuantity()).isEqualTo(200_000);
        assertThat(savedPosition.getAverageBuyPrice()).isEqualByComparingTo(BigDecimal.valueOf(250.50));

        verify(stockService).activateStockFromIssuanceApproval(stockId, 1_000_000, 200_000);
    }

    @Test
    void shouldRejectPendingIssuanceRequest() {
        UUID issuanceRequestId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        IssuanceRequest issuanceRequest = IssuanceRequest.builder()
                .id(issuanceRequestId)
                .companyId(UUID.randomUUID())
                .stockId(UUID.randomUUID())
                .submittedByUserId(UUID.randomUUID())
                .totalIssuedShares(1_000_000)
                .tradableFloatShares(200_000)
                .liquidityProviderUserId(UUID.randomUUID())
                .status(IssuanceStatus.PENDING)
                .build();

        when(issuanceRequestRepository.findById(issuanceRequestId)).thenReturn(Optional.of(issuanceRequest));
        when(issuanceRequestRepository.save(any(IssuanceRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IssuanceRequestResponse response = issuanceService.rejectIssuanceRequest(issuanceRequestId, "Issuer capitalization data is incomplete", adminUserId);

        assertThat(response.status()).isEqualTo(IssuanceStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Issuer capitalization data is incomplete");
        verify(positionRepository, never()).save(any());
        verify(stockService, never()).activateStockFromIssuanceApproval(any(), anyInt(), anyInt());
    }
}