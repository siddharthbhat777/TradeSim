package com.siddharth.tradesim_backend.listing;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.company.CompanyException;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentRole;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentStatus;
import com.siddharth.tradesim_backend.company.enums.CompanyStatus;
import com.siddharth.tradesim_backend.company.model.Company;
import com.siddharth.tradesim_backend.company.model.CompanyRepresentativeAssignment;
import com.siddharth.tradesim_backend.company.repository.CompanyRepository;
import com.siddharth.tradesim_backend.company.repository.CompanyRepresentativeAssignmentRepository;
import com.siddharth.tradesim_backend.company.service.CompanyRepresentativeAssignmentService;
import com.siddharth.tradesim_backend.exchange.ExchangeService;
import com.siddharth.tradesim_backend.exchange.model.dto.ExchangeResponse;
import com.siddharth.tradesim_backend.listing.enums.ListingStatus;
import com.siddharth.tradesim_backend.listing.model.ListingCapTableEntry;
import com.siddharth.tradesim_backend.listing.model.ListingRequest;
import com.siddharth.tradesim_backend.listing.model.dto.CapTableEntryRequest;
import com.siddharth.tradesim_backend.listing.model.dto.CreateListingRequest;
import com.siddharth.tradesim_backend.listing.model.dto.ListingRequestResponse;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.stock.enums.MarketCapCategory;
import com.siddharth.tradesim_backend.stock.enums.Sector;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.model.dto.StockResponse;
import com.siddharth.tradesim_backend.stock.service.StockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    private ListingRequestRepository listingRequestRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ExchangeService exchangeService;

    @Mock
    private CompanyRepresentativeAssignmentService companyRepresentativeAssignmentService;

    @Mock
    private CompanyRepresentativeAssignmentRepository assignmentRepository;

    @Mock
    private StockService stockService;

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private ListingService listingService;

    @Test
    void shouldSubmitListingRequestAsManagerAndSetPendingInternalReview() {
        UUID companyId = UUID.randomUUID();
        UUID managerUserId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();

        CreateListingRequest request = new CreateListingRequest(
                "INFY",
                exchangeId,
                BigDecimal.valueOf(1500.25),
                Sector.TECHNOLOGY,
                BigDecimal.TEN,
                null,
                List.of()
        );

        Company company = Company.builder()
                .id(companyId)
                .name("Infosys")
                .status(CompanyStatus.ACTIVE)
                .build();

        CompanyRepresentativeAssignment assignment = CompanyRepresentativeAssignment.builder()
                .assignmentRole(CompanyRepresentativeAssignmentRole.MANAGER)
                .build();

        ExchangeResponse exchangeResponse = new ExchangeResponse(
                exchangeId,
                "Test Exchange",
                "TST",
                "US",
                "UTC",
                "USD",
                LocalTime.of(9, 30),
                LocalTime.of(16, 0),
                com.siddharth.tradesim_backend.exchange.enums.ExchangeStatus.ACTIVE
        );

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(assignmentRepository.findByCompanyIdAndUserId(companyId, managerUserId)).thenReturn(Optional.of(assignment));
        when(stockService.existsBySymbol("INFY")).thenReturn(false);
        when(listingRequestRepository.existsBySymbolAndStatusIn("INFY", List.of(ListingStatus.PENDING_INTERNAL_REVIEW, ListingStatus.PENDING_EXCHANGE_APPROVAL))).thenReturn(false);
        when(listingRequestRepository.save(any(ListingRequest.class))).thenAnswer(invocation -> {
            ListingRequest listingRequest = invocation.getArgument(0);
            listingRequest.setId(UUID.randomUUID());
            return listingRequest;
        });
        when(exchangeService.fetchExchange(exchangeId)).thenReturn(exchangeResponse);

        ListingRequestResponse response = listingService.submitListingRequest(companyId, managerUserId, request);

        assertThat(response.symbol()).isEqualTo("INFY");
        assertThat(response.status()).isEqualTo(ListingStatus.PENDING_INTERNAL_REVIEW);
        assertThat(response.currency()).isEqualTo("USD");
        verify(companyRepresentativeAssignmentService).assertActiveRepresentativeAssignment(companyId, managerUserId);
        verify(exchangeService).assertExchangeActive(exchangeId);
    }

    @Test
    void shouldSubmitDirectListingWithCapTableAsPrimaryContactAndAutoSign() {
        UUID companyId = UUID.randomUUID();
        UUID primaryContactId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        CreateListingRequest request = new CreateListingRequest(
                "SLACK",
                exchangeId,
                BigDecimal.valueOf(120.00),
                Sector.TECHNOLOGY,
                BigDecimal.TEN,
                1000,
                List.of(new CapTableEntryRequest(targetUserId, 1000))
        );

        Company company = Company.builder().id(companyId).name("Slack Tech").status(CompanyStatus.ACTIVE).build();

        CompanyRepresentativeAssignment assignment = CompanyRepresentativeAssignment.builder()
                .assignmentRole(CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT)
                .build();

        ExchangeResponse exchangeResponse = new ExchangeResponse(
                exchangeId,
                "Test Exchange",
                "TST",
                "US",
                "UTC",
                "USD",
                LocalTime.of(9, 30),
                LocalTime.of(16, 0),
                com.siddharth.tradesim_backend.exchange.enums.ExchangeStatus.ACTIVE
        );

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(assignmentRepository.findByCompanyIdAndUserId(companyId, primaryContactId)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.existsByCompanyIdAndUserIdAndStatus(companyId, targetUserId, CompanyRepresentativeAssignmentStatus.ACTIVE)).thenReturn(true);
        when(stockService.existsBySymbol("SLACK")).thenReturn(false);
        when(listingRequestRepository.existsBySymbolAndStatusIn(eq("SLACK"), any())).thenReturn(false);
        when(listingRequestRepository.save(any(ListingRequest.class))).thenAnswer(invocation -> {
            ListingRequest listingRequest = invocation.getArgument(0);
            listingRequest.setId(UUID.randomUUID());
            return listingRequest;
        });
        when(exchangeService.fetchExchange(exchangeId)).thenReturn(exchangeResponse);

        ListingRequestResponse response = listingService.submitListingRequest(companyId, primaryContactId, request);

        assertThat(response.status()).isEqualTo(ListingStatus.PENDING_EXCHANGE_APPROVAL);
        assertThat(response.totalShares()).isEqualTo(1000);
        assertThat(response.capTable()).hasSize(1);
        assertThat(response.currency()).isEqualTo("USD");
    }

    @Test
    void shouldRejectDirectListingIfCapTableMathIsWrong() {
        UUID companyId = UUID.randomUUID();
        UUID primaryContactId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        CreateListingRequest request = new CreateListingRequest(
                "SLACK",
                exchangeId,
                BigDecimal.valueOf(120.00),
                Sector.TECHNOLOGY,
                BigDecimal.TEN,
                1000,
                List.of(new CapTableEntryRequest(targetUserId, 500))
        );

        Company company = Company.builder().id(companyId).status(CompanyStatus.ACTIVE).build();
        CompanyRepresentativeAssignment assignment = CompanyRepresentativeAssignment.builder()
                .assignmentRole(CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT).build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(assignmentRepository.findByCompanyIdAndUserId(companyId, primaryContactId)).thenReturn(Optional.of(assignment));

        BusinessException exception = assertThrows(BusinessException.class, () -> listingService.submitListingRequest(companyId, primaryContactId, request));
        assertThat(exception.getMessage()).isEqualTo("Sum of cap table quantities must equal total shares");
    }

    @Test
    void shouldRejectListingSubmissionWhenRepresentativeIsNotAssigned() {
        UUID companyId = UUID.randomUUID();
        UUID representativeUserId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();

        CreateListingRequest request = new CreateListingRequest(
                "INFY",
                exchangeId,
                BigDecimal.valueOf(1500.25),
                Sector.TECHNOLOGY,
                BigDecimal.TEN,
                null,
                List.of()
        );

        Company company = Company.builder()
                .id(companyId)
                .name("Infosys")
                .status(CompanyStatus.ACTIVE)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        doThrow(CompanyException.forbidden("Only an active assigned company representative can submit listing requests")).when(companyRepresentativeAssignmentService).assertActiveRepresentativeAssignment(companyId, representativeUserId);

        BusinessException exception = assertThrows(BusinessException.class, () -> listingService.submitListingRequest(companyId, representativeUserId, request));

        assertThat(exception.getMessage()).isEqualTo("Only an active assigned company representative can submit listing requests");
        verify(listingRequestRepository, never()).save(any());
    }

    @Test
    void shouldFetchPendingExchangeListingRequests() {
        ListingRequest request = ListingRequest.builder()
                .id(UUID.randomUUID())
                .companyId(UUID.randomUUID())
                .submittedByUserId(UUID.randomUUID())
                .symbol("INFY")
                .exchangeId(UUID.randomUUID())
                .referencePrice(BigDecimal.valueOf(1500))
                .sector(Sector.TECHNOLOGY)
                .priceBandPercent(BigDecimal.TEN)
                .status(ListingStatus.PENDING_EXCHANGE_APPROVAL)
                .capTable(List.of())
                .build();
        request.setCreatedAt(Instant.now());
        request.setUpdatedAt(Instant.now());

        Company company = Company.builder().id(request.getCompanyId()).name("Infosys").status(CompanyStatus.ACTIVE).build();
        ExchangeResponse exchangeResponse = new ExchangeResponse(
                request.getExchangeId(), "Test Exchange", "TST", "US", "UTC", "USD",
                LocalTime.of(9, 30), LocalTime.of(16, 0), com.siddharth.tradesim_backend.exchange.enums.ExchangeStatus.ACTIVE
        );

        when(listingRequestRepository.findByStatusOrderByCreatedAtDesc(ListingStatus.PENDING_EXCHANGE_APPROVAL))
                .thenReturn(List.of(request));
        when(companyRepository.findById(request.getCompanyId())).thenReturn(Optional.of(company));
        when(exchangeService.fetchExchange(request.getExchangeId())).thenReturn(exchangeResponse);

        List<ListingRequestResponse> responses = listingService.fetchPendingExchangeListingRequests();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().symbol()).isEqualTo("INFY");
        verify(listingRequestRepository).findByStatusOrderByCreatedAtDesc(ListingStatus.PENDING_EXCHANGE_APPROVAL);
    }

    @Test
    void shouldFetchPendingInternalListingRequests() {
        UUID companyId = UUID.randomUUID();
        ListingRequest request = ListingRequest.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .submittedByUserId(UUID.randomUUID())
                .symbol("INFY")
                .exchangeId(UUID.randomUUID())
                .referencePrice(BigDecimal.valueOf(1500))
                .sector(Sector.TECHNOLOGY)
                .priceBandPercent(BigDecimal.TEN)
                .status(ListingStatus.PENDING_INTERNAL_REVIEW)
                .capTable(List.of())
                .build();
        request.setCreatedAt(Instant.now());
        request.setUpdatedAt(Instant.now());

        Company company = Company.builder().id(companyId).name("Infosys").status(CompanyStatus.ACTIVE).build();
        ExchangeResponse exchangeResponse = new ExchangeResponse(
                request.getExchangeId(), "Test Exchange", "TST", "US", "UTC", "USD",
                LocalTime.of(9, 30), LocalTime.of(16, 0), com.siddharth.tradesim_backend.exchange.enums.ExchangeStatus.ACTIVE
        );

        when(listingRequestRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, ListingStatus.PENDING_INTERNAL_REVIEW))
                .thenReturn(List.of(request));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(exchangeService.fetchExchange(request.getExchangeId())).thenReturn(exchangeResponse);

        List<ListingRequestResponse> responses = listingService.fetchPendingInternalListingRequests(companyId);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().symbol()).isEqualTo("INFY");
        verify(listingRequestRepository).findByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, ListingStatus.PENDING_INTERNAL_REVIEW);
    }

    @Test
    void shouldApproveListingRequestAndInjectSharesForDirectListing() {
        UUID listingRequestId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        UUID founderUserId = UUID.randomUUID();

        ListingRequest listingRequest = ListingRequest.builder()
                .id(listingRequestId)
                .companyId(companyId)
                .submittedByUserId(UUID.randomUUID())
                .symbol("SLACK")
                .exchangeId(exchangeId)
                .referencePrice(BigDecimal.valueOf(120.00))
                .sector(Sector.TECHNOLOGY)
                .priceBandPercent(BigDecimal.TEN)
                .totalShares(1000)
                .status(ListingStatus.PENDING_EXCHANGE_APPROVAL)
                .capTable(new ArrayList<>())
                .build();

        ListingCapTableEntry capEntry = ListingCapTableEntry.builder()
                .listingRequest(listingRequest)
                .userId(founderUserId)
                .quantity(1000)
                .build();
        listingRequest.getCapTable().add(capEntry);

        Company company = Company.builder()
                .id(companyId)
                .name("Slack Tech")
                .status(CompanyStatus.ACTIVE)
                .build();

        StockResponse createdStock = new StockResponse(
                stockId,
                "SLACK",
                "Slack Tech",
                BigDecimal.valueOf(120.00),
                Sector.TECHNOLOGY,
                StockStatus.ACTIVE,
                0L,
                BigDecimal.ZERO,
                MarketCapCategory.UNKNOWN,
                "USD",
                exchangeId
        );

        ExchangeResponse exchangeResponse = new ExchangeResponse(
                exchangeId,
                "Test Exchange",
                "TST",
                "US",
                "UTC",
                "USD",
                LocalTime.of(9, 30),
                LocalTime.of(16, 0),
                com.siddharth.tradesim_backend.exchange.enums.ExchangeStatus.ACTIVE
        );

        when(listingRequestRepository.findById(listingRequestId)).thenReturn(Optional.of(listingRequest));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(stockService.createStockFromListingApproval(
                eq(companyId),
                eq(exchangeId),
                eq("SLACK"),
                eq(BigDecimal.valueOf(120.00)),
                eq(Sector.TECHNOLOGY),
                eq(BigDecimal.TEN),
                eq(1000),
                eq(StockStatus.ACTIVE)
        )).thenReturn(createdStock);

        when(positionRepository.findByUserIdAndStockId(founderUserId, stockId)).thenReturn(Optional.empty());
        when(listingRequestRepository.save(any(ListingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(exchangeService.fetchExchange(exchangeId)).thenReturn(exchangeResponse);

        ListingRequestResponse response = listingService.approveListingRequest(listingRequestId, adminUserId);

        assertThat(response.status()).isEqualTo(ListingStatus.APPROVED);
        assertThat(response.approvedStockId()).isEqualTo(stockId);
        assertThat(response.currency()).isEqualTo("USD");

        ArgumentCaptor<Position> positionCaptor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(positionCaptor.capture());

        Position savedPosition = positionCaptor.getValue();
        assertThat(savedPosition.getUserId()).isEqualTo(founderUserId);
        assertThat(savedPosition.getQuantity()).isEqualTo(1000);
        assertThat(savedPosition.getStockId()).isEqualTo(stockId);
        assertThat(savedPosition.getAverageBuyPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldRejectPendingExchangeApprovalListingRequest() {
        UUID listingRequestId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID representativeUserId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();

        ListingRequest listingRequest = ListingRequest.builder()
                .id(listingRequestId)
                .companyId(companyId)
                .submittedByUserId(representativeUserId)
                .symbol("INFY")
                .exchangeId(exchangeId)
                .referencePrice(BigDecimal.valueOf(1500.25))
                .sector(Sector.TECHNOLOGY)
                .priceBandPercent(BigDecimal.TEN)
                .status(ListingStatus.PENDING_EXCHANGE_APPROVAL)
                .capTable(new ArrayList<>())
                .build();

        ExchangeResponse exchangeResponse = new ExchangeResponse(
                exchangeId,
                "Test Exchange",
                "TST",
                "US",
                "UTC",
                "USD",
                LocalTime.of(9, 30),
                LocalTime.of(16, 0),
                com.siddharth.tradesim_backend.exchange.enums.ExchangeStatus.ACTIVE
        );

        Company company = Company.builder().id(companyId).name("Infosys").status(CompanyStatus.ACTIVE).build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(listingRequestRepository.findById(listingRequestId)).thenReturn(Optional.of(listingRequest));
        when(listingRequestRepository.save(any(ListingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(exchangeService.fetchExchange(exchangeId)).thenReturn(exchangeResponse);

        ListingRequestResponse response = listingService.rejectListingRequest(listingRequestId, "Incomplete issuer details", adminUserId);

        assertThat(response.status()).isEqualTo(ListingStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Incomplete issuer details");
        assertThat(response.reviewedByUserId()).isEqualTo(adminUserId);
        assertThat(response.approvedStockId()).isNull();
        assertThat(response.currency()).isEqualTo("USD");
    }

    @Test
    void shouldThrowWhenApprovingNonPendingExchangeListingRequest() {
        UUID listingRequestId = UUID.randomUUID();

        ListingRequest listingRequest = ListingRequest.builder()
                .id(listingRequestId)
                .symbol("INFY")
                .status(ListingStatus.PENDING_INTERNAL_REVIEW)
                .build();

        when(listingRequestRepository.findById(listingRequestId)).thenReturn(Optional.of(listingRequest));

        BusinessException exception = assertThrows(BusinessException.class, () -> listingService.approveListingRequest(listingRequestId, UUID.randomUUID()));

        assertThat(exception.getMessage()).isEqualTo("Only requests pending exchange approval can be approved by admin");
    }
}