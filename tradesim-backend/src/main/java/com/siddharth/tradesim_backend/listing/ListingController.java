package com.siddharth.tradesim_backend.listing;

import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.listing.model.dto.CreateListingRequest;
import com.siddharth.tradesim_backend.listing.model.dto.ListingRequestResponse;
import com.siddharth.tradesim_backend.listing.model.dto.RejectListingRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("listing-requests")
@RequiredArgsConstructor
public class ListingController {
    private final ListingService listingService;

    @PostMapping("{companyId}")
    @PreAuthorize("hasRole('COMPANY_REPRESENTATIVE')")
    public ResponseEntity<ListingRequestResponse> submitListingRequest(@PathVariable UUID companyId, @Valid @RequestBody CreateListingRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(listingService.submitListingRequest(companyId, principal.getUserId(), request));
    }

    @GetMapping("company/{companyId}")
    @PreAuthorize("hasRole('COMPANY_REPRESENTATIVE')")
    public ResponseEntity<List<ListingRequestResponse>> getCompanyListingRequests(@PathVariable UUID companyId) {
        return ResponseEntity.ok(listingService.fetchCompanyListingRequests(companyId));
    }

    @GetMapping("pending-exchange")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ListingRequestResponse>> getPendingExchangeListingRequests() {
        return ResponseEntity.ok(listingService.fetchPendingExchangeListingRequests());
    }

    @GetMapping("company/{companyId}/pending-internal")
    @PreAuthorize("hasRole('COMPANY_REPRESENTATIVE')")
    public ResponseEntity<List<ListingRequestResponse>> getPendingInternalListingRequests(@PathVariable UUID companyId) {
        return ResponseEntity.ok(listingService.fetchPendingInternalListingRequests(companyId));
    }

    @PutMapping("{listingRequestId}/internal-approve")
    @PreAuthorize("hasRole('COMPANY_REPRESENTATIVE')")
    public ResponseEntity<ListingRequestResponse> approveInternalListingRequest(@PathVariable UUID listingRequestId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(listingService.approveInternalListingRequest(listingRequestId, principal.getUserId()));
    }

    @PutMapping("{listingRequestId}/internal-reject")
    @PreAuthorize("hasRole('COMPANY_REPRESENTATIVE')")
    public ResponseEntity<ListingRequestResponse> rejectInternalListingRequest(@PathVariable UUID listingRequestId, @Valid @RequestBody RejectListingRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(listingService.rejectInternalListingRequest(listingRequestId, request.rejectionReason(), principal.getUserId()));
    }

    @PutMapping("{listingRequestId}/exchange-approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ListingRequestResponse> approveListingRequest(@PathVariable UUID listingRequestId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(listingService.approveListingRequest(listingRequestId, principal.getUserId()));
    }

    @PutMapping("{listingRequestId}/exchange-reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ListingRequestResponse> rejectListingRequest(@PathVariable UUID listingRequestId, @Valid @RequestBody RejectListingRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(listingService.rejectListingRequest(listingRequestId, request.rejectionReason(), principal.getUserId()));
    }
}