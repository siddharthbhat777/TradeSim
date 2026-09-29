package com.siddharth.tradesim_backend.listing;

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
import com.siddharth.tradesim_backend.listing.model.dto.CapTableEntryResponse;
import com.siddharth.tradesim_backend.listing.model.dto.CreateListingRequest;
import com.siddharth.tradesim_backend.listing.model.dto.ListingRequestResponse;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
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
    private final ExchangeService exchangeService;
    private final CompanyRepresentativeAssignmentService companyRepresentativeAssignmentService;
    private final CompanyRepresentativeAssignmentRepository assignmentRepository;
    private final StockService stockService;
    private final PositionRepository positionRepository;

    @Transactional
    public ListingRequestResponse submitListingRequest(UUID companyId, UUID actingUserId, CreateListingRequest request) {
        Company company = companyRepository.findById(companyId).orElseThrow(() -> CompanyException.notFound("Company not found"));

        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw CompanyException.conflict("Company is not active");
        }

        companyRepresentativeAssignmentService.assertActiveRepresentativeAssignment(companyId, actingUserId);
        CompanyRepresentativeAssignment assignment = assignmentRepository.findByCompanyIdAndUserId(companyId, actingUserId).orElseThrow(() -> CompanyException.forbidden("Assignment not found"));

        exchangeService.assertExchangeActive(request.exchangeId());

        if (stockService.existsBySymbol(request.symbol())) {
            throw ListingException.conflict("Stock with symbol " + request.symbol() + " already exists");
        }

        if (listingRequestRepository.existsBySymbolAndStatusIn(request.symbol(), List.of(ListingStatus.PENDING_INTERNAL_REVIEW, ListingStatus.PENDING_EXCHANGE_APPROVAL))) {
            throw ListingException.conflict("A pending listing request already exists for this symbol");
        }

        ListingStatus initialStatus = assignment.getAssignmentRole() == CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT ? ListingStatus.PENDING_EXCHANGE_APPROVAL : ListingStatus.PENDING_INTERNAL_REVIEW;

        ListingRequest listingRequest = ListingRequest.builder()
                .companyId(companyId)
                .submittedByUserId(actingUserId)
                .symbol(request.symbol())
                .exchangeId(request.exchangeId())
                .referencePrice(request.referencePrice())
                .sector(request.sector())
                .priceBandPercent(request.priceBandPercent() != null ? request.priceBandPercent() : BigDecimal.TEN)
                .totalShares(request.totalShares())
                .status(initialStatus)
                .build();

        if (request.capTable() != null && !request.capTable().isEmpty()) {
            if (request.totalShares() == null) {
                throw ListingException.badRequest("Total shares must be specified when providing a cap table");
            }

            int totalQuantity = request.capTable().stream().mapToInt(CapTableEntryRequest::quantity).sum();
            if (totalQuantity > request.totalShares()) {
                throw ListingException.badRequest("Sum of cap table quantities cannot exceed total shares");
            }

            List<ListingCapTableEntry> capTableEntries = request.capTable().stream().map(entry -> {
                boolean isRep = assignmentRepository.existsByCompanyIdAndUserIdAndStatus(companyId, entry.userId(), CompanyRepresentativeAssignmentStatus.ACTIVE);
                if (!isRep) {
                    throw ListingException.badRequest("User " + entry.userId() + " is not an active representative of the company");
                }

                return ListingCapTableEntry.builder()
                        .listingRequest(listingRequest)
                        .userId(entry.userId())
                        .quantity(entry.quantity())
                        .build();
            }).toList();

            listingRequest.getCapTable().addAll(capTableEntries);
        }

        ListingRequest saved = listingRequestRepository.save(listingRequest);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ListingRequestResponse> fetchCompanyListingRequests(UUID companyId) {
        return listingRequestRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ListingRequestResponse> fetchPendingExchangeListingRequests() {
        return listingRequestRepository.findByStatusOrderByCreatedAtDesc(ListingStatus.PENDING_EXCHANGE_APPROVAL)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ListingRequestResponse> fetchPendingInternalListingRequests(UUID companyId) {
        return listingRequestRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, ListingStatus.PENDING_INTERNAL_REVIEW)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ListingRequestResponse approveInternalListingRequest(UUID listingRequestId, UUID actingUserId) {
        ListingRequest listingRequest = listingRequestRepository.findById(listingRequestId).orElseThrow(() -> ListingException.notFound("Listing request not found"));

        if (listingRequest.getStatus() != ListingStatus.PENDING_INTERNAL_REVIEW) {
            throw ListingException.conflict("Request is not pending internal review");
        }

        companyRepresentativeAssignmentService.assertPrimaryContactAssignment(listingRequest.getCompanyId(), actingUserId);

        listingRequest.setStatus(ListingStatus.PENDING_EXCHANGE_APPROVAL);
        return toResponse(listingRequestRepository.save(listingRequest));
    }

    @Transactional
    public ListingRequestResponse rejectInternalListingRequest(UUID listingRequestId, String rejectionReason, UUID actingUserId) {
        ListingRequest listingRequest = listingRequestRepository.findById(listingRequestId).orElseThrow(() -> ListingException.notFound("Listing request not found"));

        if (listingRequest.getStatus() != ListingStatus.PENDING_INTERNAL_REVIEW) {
            throw ListingException.conflict("Request is not pending internal review");
        }

        companyRepresentativeAssignmentService.assertPrimaryContactAssignment(listingRequest.getCompanyId(), actingUserId);

        listingRequest.setStatus(ListingStatus.REJECTED);
        listingRequest.setRejectionReason(rejectionReason);
        return toResponse(listingRequestRepository.save(listingRequest));
    }

    @Transactional
    public ListingRequestResponse approveListingRequest(UUID listingRequestId, UUID adminUserId) {
        ListingRequest listingRequest = listingRequestRepository.findById(listingRequestId).orElseThrow(() -> ListingException.notFound("Listing request not found"));

        if (listingRequest.getStatus() != ListingStatus.PENDING_EXCHANGE_APPROVAL) {
            throw ListingException.conflict("Only requests pending exchange approval can be approved by admin");
        }

        Company company = companyRepository.findById(listingRequest.getCompanyId()).orElseThrow(() -> CompanyException.notFound("Company not found"));
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw CompanyException.conflict("Company is not active");
        }

        exchangeService.assertExchangeActive(listingRequest.getExchangeId());

        int capTableSum = listingRequest.getCapTable() == null ? 0 :
                listingRequest.getCapTable().stream().mapToInt(ListingCapTableEntry::getQuantity).sum();

        boolean hasIpoComponent = listingRequest.getTotalShares() == null || capTableSum < listingRequest.getTotalShares();

        StockStatus initialStockStatus = hasIpoComponent ? StockStatus.HALTED : StockStatus.ACTIVE;
        Integer initialShares = capTableSum > 0 ? capTableSum : null;

        StockResponse createdStock = stockService.createStockFromListingApproval(
                listingRequest.getCompanyId(),
                listingRequest.getExchangeId(),
                listingRequest.getSymbol(),
                listingRequest.getReferencePrice(),
                listingRequest.getSector(),
                listingRequest.getPriceBandPercent(),
                initialShares,
                initialStockStatus
        );

        if (listingRequest.getCapTable() != null && !listingRequest.getCapTable().isEmpty()) {
            for (ListingCapTableEntry entry : listingRequest.getCapTable()) {
                Position position = positionRepository.findByUserIdAndStockId(entry.getUserId(), createdStock.id())
                        .orElseGet(() -> Position.builder()
                                .userId(entry.getUserId())
                                .stockId(createdStock.id())
                                .quantity(0)
                                .lockedQuantity(0)
                                .averageBuyPrice(BigDecimal.ZERO)
                                .totalInvested(BigDecimal.ZERO)
                                .realizedPnl(BigDecimal.ZERO)
                                .build());

                position.addInvestment(BigDecimal.ZERO, entry.getQuantity());
                positionRepository.save(position);
            }
        }

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
        ListingRequest listingRequest = listingRequestRepository.findById(listingRequestId).orElseThrow(() -> ListingException.notFound("Listing request not found"));

        if (listingRequest.getStatus() != ListingStatus.PENDING_EXCHANGE_APPROVAL) {
            throw ListingException.conflict("Only requests pending exchange approval can be rejected by admin");
        }

        listingRequest.setStatus(ListingStatus.REJECTED);
        listingRequest.setReviewedByUserId(adminUserId);
        listingRequest.setReviewedAt(Instant.now());
        listingRequest.setApprovedStockId(null);
        listingRequest.setRejectionReason(rejectionReason);

        ListingRequest saved = listingRequestRepository.save(listingRequest);
        return toResponse(saved);
    }

    private ListingRequestResponse toResponse(ListingRequest listingRequest) {
        List<CapTableEntryResponse> capTableResponses = listingRequest.getCapTable().stream()
                .map(entry -> new CapTableEntryResponse(entry.getUserId(), entry.getQuantity()))
                .toList();

        ExchangeResponse exchange = exchangeService.fetchExchange(listingRequest.getExchangeId());
        Company company = companyRepository.findById(listingRequest.getCompanyId()).orElseThrow(() -> CompanyException.notFound("Company not found"));

        return new ListingRequestResponse(
                listingRequest.getId(),
                listingRequest.getCompanyId(),
                company.getName(),
                listingRequest.getSubmittedByUserId(),
                listingRequest.getSymbol(),
                listingRequest.getExchangeId(),
                exchange.name(),
                listingRequest.getReferencePrice(),
                listingRequest.getSector(),
                listingRequest.getPriceBandPercent(),
                listingRequest.getTotalShares(),
                capTableResponses,
                listingRequest.getStatus(),
                listingRequest.getReviewedByUserId(),
                listingRequest.getReviewedAt(),
                listingRequest.getApprovedStockId(),
                listingRequest.getRejectionReason(),
                exchange.currency(),
                listingRequest.getCreatedAt(),
                listingRequest.getUpdatedAt()
        );
    }
}