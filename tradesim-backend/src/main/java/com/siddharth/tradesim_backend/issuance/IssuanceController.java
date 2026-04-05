package com.siddharth.tradesim_backend.issuance;

import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.issuance.model.dto.CreateIssuanceRequest;
import com.siddharth.tradesim_backend.issuance.model.dto.IssuanceRequestResponse;
import com.siddharth.tradesim_backend.issuance.model.dto.RejectIssuanceRequest;
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
@RequestMapping("issuance-requests")
@RequiredArgsConstructor
public class IssuanceController {
    private final IssuanceService issuanceService;

    @PostMapping("{companyId}/stocks/{stockId}")
    @PreAuthorize("hasRole('COMPANY_REPRESENTATIVE')")
    public ResponseEntity<IssuanceRequestResponse> submitIssuanceRequest(@PathVariable UUID companyId, @PathVariable UUID stockId, @Valid @RequestBody CreateIssuanceRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(issuanceService.submitIssuanceRequest(companyId, stockId, principal.getUserId(), request));
    }

    @GetMapping("pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<IssuanceRequestResponse>> getPendingIssuanceRequests() {
        return ResponseEntity.ok(issuanceService.fetchPendingIssuanceRequests());
    }

    @PutMapping("{issuanceRequestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IssuanceRequestResponse> approveIssuanceRequest(@PathVariable UUID issuanceRequestId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(issuanceService.approveIssuanceRequest(issuanceRequestId, principal.getUserId()));
    }

    @PutMapping("{issuanceRequestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IssuanceRequestResponse> rejectIssuanceRequest(@PathVariable UUID issuanceRequestId, @Valid @RequestBody RejectIssuanceRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(issuanceService.rejectIssuanceRequest(issuanceRequestId, request.rejectionReason(), principal.getUserId()));
    }
}