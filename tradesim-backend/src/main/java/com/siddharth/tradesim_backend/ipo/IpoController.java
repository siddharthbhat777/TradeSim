package com.siddharth.tradesim_backend.ipo;

import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.ipo.model.dto.CreateIpoOfferRequest;
import com.siddharth.tradesim_backend.ipo.model.dto.IpoOfferResponse;
import com.siddharth.tradesim_backend.ipo.model.dto.IpoSubscriptionResponse;
import com.siddharth.tradesim_backend.ipo.model.dto.RejectIpoOfferRequest;
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
@RequestMapping("ipo-offers")
@RequiredArgsConstructor
public class IpoController {
    private final IpoService ipoService;

    @PostMapping("{companyId}/stocks/{stockId}")
    @PreAuthorize("hasRole('COMPANY_REPRESENTATIVE')")
    public ResponseEntity<IpoOfferResponse> submitIpoOffer(@PathVariable UUID companyId, @PathVariable UUID stockId, @Valid @RequestBody CreateIpoOfferRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ipoService.submitIpoOffer(companyId, stockId, principal.getUserId(), request));
    }

    @GetMapping("pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<IpoOfferResponse>> getPendingIpoOffers() {
        return ResponseEntity.ok(ipoService.fetchPendingIpoOffers());
    }

    @PutMapping("{ipoOfferId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IpoOfferResponse> approveIpoOffer(@PathVariable UUID ipoOfferId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ipoService.approveIpoOffer(ipoOfferId, principal.getUserId()));
    }

    @PutMapping("{ipoOfferId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IpoOfferResponse> rejectIpoOffer(@PathVariable UUID ipoOfferId, @Valid @RequestBody RejectIpoOfferRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ipoService.rejectIpoOffer(ipoOfferId, request.rejectionReason(), principal.getUserId()));
    }

    @GetMapping("open")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<IpoOfferResponse>> getOpenIpoOffers() {
        return ResponseEntity.ok(ipoService.fetchOpenIpoOffers());
    }

    @GetMapping("upcoming")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<IpoOfferResponse>> getUpcomingIpoOffers() {
        return ResponseEntity.ok(ipoService.fetchUpcomingIpoOffers());
    }

    @PostMapping("{ipoOfferId}/subscriptions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<IpoSubscriptionResponse> subscribeToIpo(@PathVariable UUID ipoOfferId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ipoService.subscribeToIpo(ipoOfferId, principal.getUserId()));
    }

    @GetMapping("subscriptions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<IpoSubscriptionResponse>> getMySubscriptions(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ipoService.fetchMySubscriptions(principal.getUserId()));
    }

    @GetMapping("{ipoOfferId}/subscriptions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<IpoSubscriptionResponse>> getSubscriptionsForOffer(@PathVariable UUID ipoOfferId) {
        return ResponseEntity.ok(ipoService.fetchSubscriptionsForOffer(ipoOfferId));
    }

    @PutMapping("{ipoOfferId}/finalize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IpoOfferResponse> finalizeIpoOffer(@PathVariable UUID ipoOfferId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ipoService.finalizeIpoOffer(ipoOfferId, principal.getUserId()));
    }
}