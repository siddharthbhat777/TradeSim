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

    @GetMapping("pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ListingRequestResponse>> getPendingListingRequests() {
        return ResponseEntity.ok(listingService.fetchPendingListingRequests());
    }

    @PutMapping("{listingRequestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ListingRequestResponse> approveListingRequest(@PathVariable UUID listingRequestId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(listingService.approveListingRequest(listingRequestId, principal.getUserId()));
    }

    @PutMapping("{listingRequestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ListingRequestResponse> rejectListingRequest(@PathVariable UUID listingRequestId, @Valid @RequestBody RejectListingRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(listingService.rejectListingRequest(listingRequestId, request.rejectionReason(), principal.getUserId()));
    }
}