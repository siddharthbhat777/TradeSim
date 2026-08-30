package com.siddharth.tradesim_backend.listing;

import com.siddharth.tradesim_backend.company.CompanyException;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.company.enums.CompanyStatus;
import com.siddharth.tradesim_backend.company.model.Company;
import com.siddharth.tradesim_backend.company.repository.CompanyRepository;
import com.siddharth.tradesim_backend.company.service.CompanyRepresentativeAssignmentService;
import com.siddharth.tradesim_backend.exchange.ExchangeException;
import com.siddharth.tradesim_backend.exchange.ExchangeService;
import com.siddharth.tradesim_backend.listing.enums.ListingStatus;
import com.siddharth.tradesim_backend.listing.model.ListingRequest;
import com.siddharth.tradesim_backend.listing.model.dto.CreateListingRequest;
import com.siddharth.tradesim_backend.listing.model.dto.ListingRequestResponse;
import com.siddharth.tradesim_backend.stock.enums.MarketCapCategory;
import com.siddharth.tradesim_backend.stock.enums.Sector;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.model.dto.StockResponse;
import com.siddharth.tradesim_backend.stock.service.StockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private StockService stockService;

    @InjectMocks
    private ListingService listingService;

    @Test
    void shouldSubmitListingRequestWhenRepresentativeAssignmentIsActive() {
        UUID companyId = UUID.randomUUID();
        UUID representativeUserId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();

        CreateListingRequest request = new CreateListingRequest(
                "INFY",
                exchangeId,
                BigDecimal.valueOf(1500.25),
                Sector.TECHNOLOGY,
                null
        );

        Company company = Company.builder()
                .id(companyId)
                .name("Infosys")
                .status(CompanyStatus.ACTIVE)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(stockService.existsBySymbol("INFY")).thenReturn(false);
        when(listingRequestRepository.existsBySymbolAndStatus("INFY", ListingStatus.PENDING)).thenReturn(false);
        when(listingRequestRepository.save(any(ListingRequest.class))).thenAnswer(invocation -> {
            ListingRequest listingRequest = invocation.getArgument(0);
            listingRequest.setId(UUID.randomUUID());
            return listingRequest;
        });

        ListingRequestResponse response = listingService.submitListingRequest(companyId, representativeUserId, request);

        assertThat(response.symbol()).isEqualTo("INFY");
        assertThat(response.status()).isEqualTo(ListingStatus.PENDING);
        assertThat(response.priceBandPercent()).isEqualByComparingTo(BigDecimal.TEN);
        verify(companyRepresentativeAssignmentService).assertActiveRepresentativeAssignment(companyId, representativeUserId);
        verify(exchangeService).assertExchangeActive(exchangeId);
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
                BigDecimal.TEN
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
    void shouldApproveListingRequestAndCreateHaltedStock() {
        UUID listingRequestId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID representativeUserId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        ListingRequest listingRequest = ListingRequest.builder()
                .id(listingRequestId)
                .companyId(companyId)
                .submittedByUserId(representativeUserId)
                .symbol("INFY")
                .exchangeId(exchangeId)
                .referencePrice(BigDecimal.valueOf(1500.25))
                .sector(Sector.TECHNOLOGY)
                .priceBandPercent(BigDecimal.TEN)
                .status(ListingStatus.PENDING)
                .build();

        Company company = Company.builder()
                .id(companyId)
                .name("Infosys")
                .status(CompanyStatus.ACTIVE)
                .build();

        StockResponse createdStock = new StockResponse(
                stockId,
                "INFY",
                "Infosys",
                BigDecimal.valueOf(1500.25),
                Sector.TECHNOLOGY,
                StockStatus.HALTED,
                0L,
                BigDecimal.ZERO,
                MarketCapCategory.UNKNOWN
        );

        when(listingRequestRepository.findById(listingRequestId)).thenReturn(Optional.of(listingRequest));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(stockService.createStockFromListingApproval(
                eq(companyId),
                eq(exchangeId),
                eq("INFY"),
                eq(BigDecimal.valueOf(1500.25)),
                eq(Sector.TECHNOLOGY),
                eq(BigDecimal.TEN)
        )).thenReturn(createdStock);
        when(listingRequestRepository.save(any(ListingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ListingRequestResponse response = listingService.approveListingRequest(listingRequestId, adminUserId);

        assertThat(response.status()).isEqualTo(ListingStatus.APPROVED);
        assertThat(response.approvedStockId()).isEqualTo(stockId);
        assertThat(response.reviewedByUserId()).isEqualTo(adminUserId);
        verify(stockService).createStockFromListingApproval(
                eq(companyId),
                eq(exchangeId),
                eq("INFY"),
                eq(BigDecimal.valueOf(1500.25)),
                eq(Sector.TECHNOLOGY),
                eq(BigDecimal.TEN)
        );
        verify(exchangeService).assertExchangeActive(exchangeId);
    }

    @Test
    void shouldRejectPendingListingRequest() {
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
                .status(ListingStatus.PENDING)
                .build();

        when(listingRequestRepository.findById(listingRequestId)).thenReturn(Optional.of(listingRequest));
        when(listingRequestRepository.save(any(ListingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ListingRequestResponse response = listingService.rejectListingRequest(listingRequestId, "Incomplete issuer details", adminUserId);

        assertThat(response.status()).isEqualTo(ListingStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Incomplete issuer details");
        assertThat(response.reviewedByUserId()).isEqualTo(adminUserId);
        assertThat(response.approvedStockId()).isNull();
    }

    @Test
    void shouldThrowWhenApprovingNonPendingListingRequest() {
        UUID listingRequestId = UUID.randomUUID();

        ListingRequest listingRequest = ListingRequest.builder()
                .id(listingRequestId)
                .symbol("INFY")
                .status(ListingStatus.REJECTED)
                .build();

        when(listingRequestRepository.findById(listingRequestId)).thenReturn(Optional.of(listingRequest));

        BusinessException exception = assertThrows(BusinessException.class, () -> listingService.approveListingRequest(listingRequestId, UUID.randomUUID()));

        assertThat(exception.getMessage()).isEqualTo("Only pending listing requests can be reviewed");
    }

    @Test
    void shouldRejectListingSubmissionWhenExchangeIsInactive() {
        UUID companyId = UUID.randomUUID();
        UUID representativeUserId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();

        CreateListingRequest request = new CreateListingRequest(
                "INFY",
                exchangeId,
                BigDecimal.valueOf(1500.25),
                Sector.TECHNOLOGY,
                BigDecimal.TEN
        );

        Company company = Company.builder()
                .id(companyId)
                .name("Infosys")
                .status(CompanyStatus.ACTIVE)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        doThrow(ExchangeException.conflict("Exchange is not active")).when(exchangeService).assertExchangeActive(exchangeId);

        BusinessException exception = assertThrows(BusinessException.class, () -> listingService.submitListingRequest(companyId, representativeUserId, request));

        assertThat(exception.getMessage()).isEqualTo("Exchange is not active");
        verify(listingRequestRepository, never()).save(any());
    }
}