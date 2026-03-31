package com.siddharth.tradesim_backend.company.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentStatus;
import com.siddharth.tradesim_backend.company.model.Company;
import com.siddharth.tradesim_backend.company.model.CompanyRepresentativeAssignment;
import com.siddharth.tradesim_backend.company.model.dto.CompanyRepresentativeAssignmentResponse;
import com.siddharth.tradesim_backend.company.repository.CompanyRepresentativeAssignmentRepository;
import com.siddharth.tradesim_backend.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyRepresentativeAssignmentService {
    private final CompanyRepresentativeAssignmentRepository companyRepresentativeAssignmentRepository;
    private final CompanyRepository companyRepository;
    private final AuthRepository authRepository;

    @Transactional
    public CompanyRepresentativeAssignmentResponse assignRepresentative(UUID companyId, UUID userId, UUID adminUserId) {
        Company company = companyRepository.findById(companyId).orElseThrow(() -> new BusinessException("Company not found"));
        User user = authRepository.findById(userId).orElseThrow(() -> new BusinessException("User not found"));

        if (user.getRole() != Role.COMPANY_REPRESENTATIVE) {
            throw new BusinessException("User is not a company representative");
        }

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Company representative account must be active");
        }

        CompanyRepresentativeAssignment assignment = companyRepresentativeAssignmentRepository
                .findByCompanyIdAndUserId(company.getId(), user.getId())
                .map(existing -> {
                    if (existing.getStatus() == CompanyRepresentativeAssignmentStatus.ACTIVE) {
                        throw new BusinessException("Company representative is already assigned to this company");
                    }

                    existing.setStatus(CompanyRepresentativeAssignmentStatus.ACTIVE);
                    existing.setRevokedAt(null);
                    existing.setAssignedByAdminId(adminUserId);
                    return existing;
                })
                .orElseGet(() -> CompanyRepresentativeAssignment.builder()
                        .companyId(company.getId())
                        .userId(user.getId())
                        .assignedByAdminId(adminUserId)
                        .status(CompanyRepresentativeAssignmentStatus.ACTIVE)
                        .build()
                );

        CompanyRepresentativeAssignment saved = companyRepresentativeAssignmentRepository.save(assignment);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CompanyRepresentativeAssignmentResponse> fetchActiveAssignments(UUID companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new BusinessException("Company not found");
        }

        return companyRepresentativeAssignmentRepository.findByCompanyIdAndStatus(companyId, CompanyRepresentativeAssignmentStatus.ACTIVE).stream().map(this::toResponse).toList();
    }

    @Transactional
    public CompanyRepresentativeAssignmentResponse revokeRepresentative(UUID companyId, UUID userId) {
        CompanyRepresentativeAssignment assignment = companyRepresentativeAssignmentRepository.findByCompanyIdAndUserId(companyId, userId).orElseThrow(() -> new BusinessException("Company representative assignment not found"));

        if (assignment.getStatus() == CompanyRepresentativeAssignmentStatus.REVOKED) {
            throw new BusinessException("Company representative assignment is already revoked");
        }

        assignment.setStatus(CompanyRepresentativeAssignmentStatus.REVOKED);
        assignment.setRevokedAt(Instant.now());

        CompanyRepresentativeAssignment saved = companyRepresentativeAssignmentRepository.save(assignment);
        return toResponse(saved);
    }

    private CompanyRepresentativeAssignmentResponse toResponse(CompanyRepresentativeAssignment assignment) {
        return new CompanyRepresentativeAssignmentResponse(
                assignment.getId(),
                assignment.getCompanyId(),
                assignment.getUserId(),
                assignment.getAssignedByAdminId(),
                assignment.getStatus(),
                assignment.getRevokedAt()
        );
    }
}