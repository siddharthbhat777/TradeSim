package com.siddharth.tradesim_backend.company.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.company.enums.CompanyManagerAssignmentStatus;
import com.siddharth.tradesim_backend.company.model.Company;
import com.siddharth.tradesim_backend.company.model.CompanyManagerAssignment;
import com.siddharth.tradesim_backend.company.model.dto.CompanyManagerAssignmentResponse;
import com.siddharth.tradesim_backend.company.repository.CompanyManagerAssignmentRepository;
import com.siddharth.tradesim_backend.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyManagerAssignmentService {
    private final CompanyManagerAssignmentRepository companyManagerAssignmentRepository;
    private final CompanyRepository companyRepository;
    private final AuthRepository authRepository;

    @Transactional
    public CompanyManagerAssignmentResponse assignManager(UUID companyId, UUID userId, UUID adminUserId) {
        Company company = companyRepository.findById(companyId).orElseThrow(() -> new BusinessException("Company not found"));
        User user = authRepository.findById(userId).orElseThrow(() -> new BusinessException("User not found"));

        if (user.getRole() != Role.COMPANY_MANAGER) {
            throw new BusinessException("User is not a company manager");
        }

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Company manager account must be active");
        }

        CompanyManagerAssignment assignment = companyManagerAssignmentRepository
                .findByCompanyIdAndUserId(company.getId(), user.getId())
                .map(existing -> {
                    if (existing.getStatus() == CompanyManagerAssignmentStatus.ACTIVE) {
                        throw new BusinessException("Company manager is already assigned to this company");
                    }

                    existing.setStatus(CompanyManagerAssignmentStatus.ACTIVE);
                    existing.setRevokedAt(null);
                    existing.setAssignedByAdminId(adminUserId);
                    return existing;
                })
                .orElseGet(() -> CompanyManagerAssignment.builder()
                        .companyId(company.getId())
                        .userId(user.getId())
                        .assignedByAdminId(adminUserId)
                        .status(CompanyManagerAssignmentStatus.ACTIVE)
                        .build()
                );

        CompanyManagerAssignment saved = companyManagerAssignmentRepository.save(assignment);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CompanyManagerAssignmentResponse> fetchActiveAssignments(UUID companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new BusinessException("Company not found");
        }

        return companyManagerAssignmentRepository.findByCompanyIdAndStatus(companyId, CompanyManagerAssignmentStatus.ACTIVE).stream().map(this::toResponse).toList();
    }

    @Transactional
    public CompanyManagerAssignmentResponse revokeManager(UUID companyId, UUID userId) {
        CompanyManagerAssignment assignment = companyManagerAssignmentRepository.findByCompanyIdAndUserId(companyId, userId).orElseThrow(() -> new BusinessException("Company manager assignment not found"));

        if (assignment.getStatus() == CompanyManagerAssignmentStatus.REVOKED) {
            throw new BusinessException("Company manager assignment is already revoked");
        }

        assignment.setStatus(CompanyManagerAssignmentStatus.REVOKED);
        assignment.setRevokedAt(Instant.now());

        CompanyManagerAssignment saved = companyManagerAssignmentRepository.save(assignment);
        return toResponse(saved);
    }

    private CompanyManagerAssignmentResponse toResponse(CompanyManagerAssignment assignment) {
        return new CompanyManagerAssignmentResponse(
                assignment.getId(),
                assignment.getCompanyId(),
                assignment.getUserId(),
                assignment.getAssignedByAdminId(),
                assignment.getStatus(),
                assignment.getRevokedAt()
        );
    }
}