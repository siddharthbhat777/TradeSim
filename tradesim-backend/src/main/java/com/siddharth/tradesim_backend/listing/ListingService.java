package com.siddharth.tradesim_backend.listing;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.company.enums.CompanyStatus;
import com.siddharth.tradesim_backend.company.model.Company;
import com.siddharth.tradesim_backend.company.repository.CompanyRepository;
import com.siddharth.tradesim_backend.company.service.CompanyRepresentativeAssignmentService;
import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.listing.enums.ListingStatus;
import com.siddharth.tradesim_backend.listing.model.ListingRequest;
import com.siddharth.tradesim_backend.listing.model.dto.CreateListingRequest;
import com.siddharth.tradesim_backend.listing.model.dto.ListingRequestResponse;
import com.siddharth.tradesim_backend.stock.model.dto.StockResponse;
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
public class ListingService {
    private final ListingRequestRepository listingRequestRepository;
    private final CompanyRepository companyRepository;
    private final ExchangeRepository exchangeRepository;
    private final CompanyRepresentativeAssignmentService companyRepresentativeAssignmentService;
    private final StockService stockService;

    @Transactional
    public ListingRequestResponse submitListingRequest(UUID companyId, UUID actingUserId, CreateListingRequest request) {
        Company company = companyRepository.findById(companyId).orElseThrow(() -> new BusinessException("Company not found"));

        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new BusinessException("Company is not active");
        }

        companyRepresentativeAssignmentService.assertActiveRepresentativeAssignment(companyId, actingUserId);

        exchangeRepository.findById(request.exchangeId()).orElseThrow(() -> new BusinessException("Exchange not found"));

        if (stockService.existsBySymbol(request.symbol())) {
            throw new BusinessException("Stock with symbol " + request.symbol() + " already exists");
        }

        if (listingRequestRepository.existsBySymbolAndStatus(request.symbol(), ListingStatus.PENDING)) {
            throw new BusinessException("A pending listing request already exists for this symbol");
        }

        ListingRequest listingRequest = ListingRequest.builder()
                .companyId(companyId)
                .submittedByUserId(actingUserId)
                .symbol(request.symbol())
                .exchangeId(request.exchangeId())
                .referencePrice(request.referencePrice())
                .sector(request.sector())
                .priceBandPercent(request.priceBandPercent() != null ? request.priceBandPercent() : BigDecimal.TEN)
                .status(ListingStatus.PENDING)
                .build();

        ListingRequest saved = listingRequestRepository.save(listingRequest);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ListingRequestResponse> fetchPendingListingRequests() {
        return listingRequestRepository.findByStatusOrderByCreatedAtAsc(ListingStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ListingRequestResponse approveListingRequest(UUID listingRequestId, UUID adminUserId) {
        ListingRequest listingRequest = findPendingListingRequest(listingRequestId);

        Company company = companyRepository.findById(listingRequest.getCompanyId()).orElseThrow(() -> new BusinessException("Company not found"));
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new BusinessException("Company is not active");
        }

        exchangeRepository.findById(listingRequest.getExchangeId()).orElseThrow(() -> new BusinessException("Exchange not found"));

        StockResponse createdStock = stockService.createStockFromListingApproval(
                listingRequest.getCompanyId(),
                listingRequest.getExchangeId(),
                listingRequest.getSymbol(),
                listingRequest.getReferencePrice(),
                listingRequest.getSector(),
                listingRequest.getPriceBandPercent()
        );

        listingRequest.setStatus(ListingStatus.APPROVED);
        listingRequest.setReviewedByUserId(adminUserId);
        listingRequest.setReviewedAt(Instant.now());
        listingRequest.setApprovedStockId(createdStock.id());
        listingRequest.setRejectionReason(null);

        ListingRequest saved = listingRequestRepository.save(listingRequest);
        return toResponse(saved);
    }

    @Transactional
    public ListingRequestResponse rejectListingRequest(UUID listingRequestId, String rejectionReason, UUID adminUserId) {
        ListingRequest listingRequest = findPendingListingRequest(listingRequestId);

        listingRequest.setStatus(ListingStatus.REJECTED);
        listingRequest.setReviewedByUserId(adminUserId);
        listingRequest.setReviewedAt(Instant.now());
        listingRequest.setApprovedStockId(null);
        listingRequest.setRejectionReason(rejectionReason);

        ListingRequest saved = listingRequestRepository.save(listingRequest);
        return toResponse(saved);
    }

    private ListingRequest findPendingListingRequest(UUID listingRequestId) {
        ListingRequest listingRequest = listingRequestRepository.findById(listingRequestId).orElseThrow(() -> new BusinessException("Listing request not found"));

        if (listingRequest.getStatus() != ListingStatus.PENDING) {
            throw new BusinessException("Only pending listing requests can be reviewed");
        }

        return listingRequest;
    }

    private ListingRequestResponse toResponse(ListingRequest listingRequest) {
        return new ListingRequestResponse(
                listingRequest.getId(),
                listingRequest.getCompanyId(),
                listingRequest.getSubmittedByUserId(),
                listingRequest.getSymbol(),
                listingRequest.getExchangeId(),
                listingRequest.getReferencePrice(),
                listingRequest.getSector(),
                listingRequest.getPriceBandPercent(),
                listingRequest.getStatus(),
                listingRequest.getReviewedByUserId(),
                listingRequest.getReviewedAt(),
                listingRequest.getApprovedStockId(),
                listingRequest.getRejectionReason(),
                listingRequest.getCreatedAt(),
                listingRequest.getUpdatedAt()
        );
    }
}