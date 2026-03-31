package com.siddharth.tradesim_backend.company;

import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.company.model.dto.AssignCompanyManagerRequest;
import com.siddharth.tradesim_backend.company.model.dto.ChangeCompanyStatusRequest;
import com.siddharth.tradesim_backend.company.model.dto.CompanyManagerAssignmentResponse;
import com.siddharth.tradesim_backend.company.model.dto.CompanyResponse;
import com.siddharth.tradesim_backend.company.model.dto.CreateCompanyRequest;
import com.siddharth.tradesim_backend.company.service.CompanyManagerAssignmentService;
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
    private final CompanyManagerAssignmentService companyManagerAssignmentService;

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

    @PutMapping("{companyId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyResponse> changeStatus(@PathVariable UUID companyId, @Valid @RequestBody ChangeCompanyStatusRequest request) {
        return ResponseEntity.ok(companyService.changeStatus(companyId, request.status()));
    }

    @PostMapping("{companyId}/managers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyManagerAssignmentResponse> assignManager(@PathVariable UUID companyId, @Valid @RequestBody AssignCompanyManagerRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyManagerAssignmentService.assignManager(companyId, request.userId(), principal.getUserId()));
    }

    @GetMapping("{companyId}/managers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CompanyManagerAssignmentResponse>> getManagers(@PathVariable UUID companyId) {
        return ResponseEntity.ok(companyManagerAssignmentService.fetchActiveAssignments(companyId));
    }

    @DeleteMapping("{companyId}/managers/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyManagerAssignmentResponse> revokeManager(@PathVariable UUID companyId, @PathVariable UUID userId) {
        return ResponseEntity.ok(companyManagerAssignmentService.revokeManager(companyId, userId));
    }
}