package com.siddharth.tradesim_backend.company;

import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.company.model.dto.AssignCompanyRepresentativeRequest;
import com.siddharth.tradesim_backend.company.model.dto.ChangeCompanyStatusRequest;
import com.siddharth.tradesim_backend.company.model.dto.CompanyOnboardingResponse;
import com.siddharth.tradesim_backend.company.model.dto.CompanyRepresentativeAssignmentResponse;
import com.siddharth.tradesim_backend.company.model.dto.CompanyResponse;
import com.siddharth.tradesim_backend.company.model.dto.CreateCompanyOnboardingRequest;
import com.siddharth.tradesim_backend.company.model.dto.CreateCompanyRequest;
import com.siddharth.tradesim_backend.company.model.dto.PrimaryContactTransferResponse;
import com.siddharth.tradesim_backend.company.model.dto.TransferPrimaryContactRequest;
import com.siddharth.tradesim_backend.company.service.CompanyOnboardingService;
import com.siddharth.tradesim_backend.company.service.CompanyRepresentativeAssignmentService;
import com.siddharth.tradesim_backend.company.service.CompanyService;
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
@RequestMapping("companies")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService companyService;
    private final CompanyRepresentativeAssignmentService companyRepresentativeAssignmentService;
    private final CompanyOnboardingService companyOnboardingService;

    @GetMapping
    public ResponseEntity<List<CompanyResponse>> getCompanies() {
        return ResponseEntity.ok(companyService.fetchCompanies());
    }

    @GetMapping("{companyId}")
    public ResponseEntity<CompanyResponse> getCompany(@PathVariable UUID companyId) {
        return ResponseEntity.ok(companyService.fetchCompany(companyId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyResponse> createCompany(@Valid @RequestBody CreateCompanyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.createCompany(request));
    }

    @PostMapping("onboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyOnboardingResponse> onboardCompany(@Valid @RequestBody CreateCompanyOnboardingRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyOnboardingService.onboardCompany(request, principal.getUserId()));
    }

    @PutMapping("{companyId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyResponse> changeStatus(@PathVariable UUID companyId, @Valid @RequestBody ChangeCompanyStatusRequest request) {
        return ResponseEntity.ok(companyService.changeStatus(companyId, request.status()));
    }

    @PostMapping("{companyId}/representatives")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_REPRESENTATIVE')")
    public ResponseEntity<CompanyRepresentativeAssignmentResponse> assignRepresentative(@PathVariable UUID companyId, @Valid @RequestBody AssignCompanyRepresentativeRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyRepresentativeAssignmentService.assignRepresentative(companyId, request.userId(), principal.getUserId()));
    }

    @GetMapping("{companyId}/representatives")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_REPRESENTATIVE')")
    public ResponseEntity<List<CompanyRepresentativeAssignmentResponse>> getRepresentatives(@PathVariable UUID companyId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(companyRepresentativeAssignmentService.fetchActiveAssignments(companyId, principal.getUserId()));
    }

    @DeleteMapping("{companyId}/representatives/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_REPRESENTATIVE')")
    public ResponseEntity<CompanyRepresentativeAssignmentResponse> revokeRepresentative(@PathVariable UUID companyId, @PathVariable UUID userId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(companyRepresentativeAssignmentService.revokeRepresentative(companyId, userId, principal.getUserId()));
    }

    @PutMapping("{companyId}/representatives/primary-contact")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPANY_REPRESENTATIVE')")
    public ResponseEntity<PrimaryContactTransferResponse> transferPrimaryContact(@PathVariable UUID companyId, @Valid @RequestBody TransferPrimaryContactRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(companyRepresentativeAssignmentService.transferPrimaryContact(companyId, request.newPrimaryContactUserId(), principal.getUserId()));
    }
}