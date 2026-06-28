package com.siddharth.tradesim_backend.company.service;

import com.siddharth.tradesim_backend.auth.repository.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.company.CompanyException;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentRole;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentStatus;
import com.siddharth.tradesim_backend.company.model.CompanyRepresentativeAssignment;
import com.siddharth.tradesim_backend.company.model.dto.CompanyRepresentativeAssignmentResponse;
import com.siddharth.tradesim_backend.company.model.dto.PrimaryContactTransferResponse;
import com.siddharth.tradesim_backend.company.repository.CompanyRepresentativeAssignmentRepository;
import com.siddharth.tradesim_backend.company.repository.CompanyRepository;
import com.siddharth.tradesim_backend.user.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyRepresentativeAssignmentService {
    private final CompanyRepresentativeAssignmentRepository companyRepresentativeAssignmentRepository;
    private final CompanyRepository companyRepository;
    private final AuthRepository authRepository;

    @Transactional
    public CompanyRepresentativeAssignmentResponse assignRepresentative(UUID companyId, UUID targetUserId, UUID actingUserId) {
        if (!companyRepository.existsById(companyId)) {
            throw CompanyException.notFound("Company not found");
        }

        User targetUser = authRepository.findById(targetUserId).orElseThrow(() -> UserException.notFound("User not found"));

        assertUserIsAssignableRepresentative(targetUser);
        assertCanManageRepresentatives(companyId, actingUserId);

        CompanyRepresentativeAssignmentRole assignmentRole = resolveAssignmentRoleForNewActiveAssignment(companyId);

        CompanyRepresentativeAssignment assignment = companyRepresentativeAssignmentRepository
                .findByCompanyIdAndUserId(companyId, targetUserId)
                .map(existing -> reactivateAssignment(existing, actingUserId, assignmentRole))
                .orElseGet(() -> CompanyRepresentativeAssignment.builder()
                        .companyId(companyId)
                        .userId(targetUserId)
                        .assignedByUserId(actingUserId)
                        .assignmentRole(assignmentRole)
                        .status(CompanyRepresentativeAssignmentStatus.ACTIVE)
                        .build());

        CompanyRepresentativeAssignment saved = companyRepresentativeAssignmentRepository.save(assignment);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CompanyRepresentativeAssignmentResponse> fetchActiveAssignments(UUID companyId, UUID actingUserId) {
        if (!companyRepository.existsById(companyId)) {
            throw CompanyException.notFound("Company not found");
        }

        assertCanViewRepresentatives(companyId, actingUserId);

        return companyRepresentativeAssignmentRepository.findByCompanyIdAndStatus(companyId, CompanyRepresentativeAssignmentStatus.ACTIVE)
                .stream()
                .sorted(Comparator.comparing((CompanyRepresentativeAssignment assignment) -> assignment.getAssignmentRole() != CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public void assertActiveRepresentativeAssignment(UUID companyId, UUID actingUserId) {
        User actingUser = authRepository.findById(actingUserId).orElseThrow(() -> UserException.notFound("User not found"));

        if (actingUser.getRole() != Role.COMPANY_REPRESENTATIVE || actingUser.getAccountStatus() != AccountStatus.ACTIVE) {
            throw CompanyException.forbidden("Only an active assigned company representative can submit listing requests");
        }

        boolean hasActiveAssignment = companyRepresentativeAssignmentRepository.existsByCompanyIdAndUserIdAndStatus(
                companyId,
                actingUserId,
                CompanyRepresentativeAssignmentStatus.ACTIVE
        );

        if (!hasActiveAssignment) {
            throw CompanyException.forbidden("Only an active assigned company representative can submit listing requests");
        }
    }

    @Transactional(readOnly = true)
    public void assertPrimaryContactAssignment(UUID companyId, UUID actingUserId) {
        assertPrimaryContactAssignment(companyId, actingUserId, "Only an active primary contact can perform this company action");
    }

    @Transactional(readOnly = true)
    public void assertPrimaryContactAssignment(UUID companyId, UUID actingUserId, String errorMessage) {
        User actingUser = authRepository.findById(actingUserId).orElseThrow(() -> UserException.notFound("User not found"));

        if (actingUser.getRole() != Role.COMPANY_REPRESENTATIVE || actingUser.getAccountStatus() != AccountStatus.ACTIVE) {
            throw CompanyException.forbidden(errorMessage);
        }

        CompanyRepresentativeAssignment actingAssignment = companyRepresentativeAssignmentRepository
                .findByCompanyIdAndUserId(companyId, actingUserId)
                .filter(assignment -> assignment.getStatus() == CompanyRepresentativeAssignmentStatus.ACTIVE)
                .orElseThrow(() -> CompanyException.forbidden(errorMessage));

        if (actingAssignment.getAssignmentRole() != CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT) {
            throw CompanyException.forbidden(errorMessage);
        }
    }

    @Transactional
    public CompanyRepresentativeAssignmentResponse revokeRepresentative(UUID companyId, UUID targetUserId, UUID actingUserId) {
        assertCanManageRepresentatives(companyId, actingUserId);

        CompanyRepresentativeAssignment assignment = companyRepresentativeAssignmentRepository
                .findByCompanyIdAndUserId(companyId, targetUserId)
                .orElseThrow(() -> CompanyException.notFound("Company representative assignment not found"));

        if (assignment.getStatus() == CompanyRepresentativeAssignmentStatus.REVOKED) {
            throw CompanyException.conflict("Company representative assignment is already revoked");
        }

        if (assignment.getAssignmentRole() == CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT) {
            throw CompanyException.conflict("Transfer primary contact before revoking the current primary contact");
        }

        assignment.setStatus(CompanyRepresentativeAssignmentStatus.REVOKED);
        assignment.setRevokedAt(Instant.now());
        assignment.setRevokedByUserId(actingUserId);

        CompanyRepresentativeAssignment saved = companyRepresentativeAssignmentRepository.save(assignment);
        return toResponse(saved);
    }

    @Transactional
    public PrimaryContactTransferResponse transferPrimaryContact(UUID companyId, UUID newPrimaryContactUserId, UUID actingUserId) {
        assertCanManageRepresentatives(companyId, actingUserId);

        CompanyRepresentativeAssignment currentPrimaryContact = companyRepresentativeAssignmentRepository
                .findByCompanyIdAndStatusAndAssignmentRole(
                        companyId,
                        CompanyRepresentativeAssignmentStatus.ACTIVE,
                        CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT
                )
                .orElseThrow(() -> CompanyException.notFound("Active primary contact not found"));

        CompanyRepresentativeAssignment newPrimaryContact = companyRepresentativeAssignmentRepository
                .findByCompanyIdAndUserId(companyId, newPrimaryContactUserId)
                .filter(assignment -> assignment.getStatus() == CompanyRepresentativeAssignmentStatus.ACTIVE)
                .orElseThrow(() -> CompanyException.notFound("Target company representative assignment not found"));

        if (newPrimaryContact.getAssignmentRole() == CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT) {
            throw CompanyException.conflict("Target company representative is already the primary contact");
        }

        currentPrimaryContact.setAssignmentRole(CompanyRepresentativeAssignmentRole.MANAGER);
        newPrimaryContact.setAssignmentRole(CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT);

        companyRepresentativeAssignmentRepository.save(currentPrimaryContact);
        companyRepresentativeAssignmentRepository.save(newPrimaryContact);

        return new PrimaryContactTransferResponse(
                companyId,
                currentPrimaryContact.getUserId(),
                newPrimaryContact.getUserId(),
                actingUserId,
                Instant.now()
        );
    }

    private CompanyRepresentativeAssignment reactivateAssignment(CompanyRepresentativeAssignment existing, UUID actingUserId, CompanyRepresentativeAssignmentRole assignmentRole) {
        if (existing.getStatus() == CompanyRepresentativeAssignmentStatus.ACTIVE) {
            throw CompanyException.conflict("Company representative is already assigned to this company");
        }

        existing.setStatus(CompanyRepresentativeAssignmentStatus.ACTIVE);
        existing.setAssignedByUserId(actingUserId);
        existing.setAssignmentRole(assignmentRole);
        existing.setRevokedAt(null);
        existing.setRevokedByUserId(null);
        return existing;
    }

    private void assertUserIsAssignableRepresentative(User targetUser) {
        if (targetUser.getRole() != Role.COMPANY_REPRESENTATIVE) {
            throw CompanyException.conflict("User is not a company representative");
        }

        if (targetUser.getAccountStatus() != AccountStatus.ACTIVE) {
            throw CompanyException.conflict("Company representative account must be active");
        }
    }

    private void assertCanViewRepresentatives(UUID companyId, UUID actingUserId) {
        User actingUser = authRepository.findById(actingUserId).orElseThrow(() -> UserException.notFound("User not found"));

        if (actingUser.getRole() == Role.ADMIN) {
            return;
        }

        if (actingUser.getRole() != Role.COMPANY_REPRESENTATIVE || actingUser.getAccountStatus() != AccountStatus.ACTIVE) {
            throw CompanyException.forbidden("Only admin or assigned company representative can view company representatives");
        }

        boolean hasActiveAssignment = companyRepresentativeAssignmentRepository.existsByCompanyIdAndUserIdAndStatus(
                companyId,
                actingUserId,
                CompanyRepresentativeAssignmentStatus.ACTIVE
        );

        if (!hasActiveAssignment) {
            throw CompanyException.forbidden("Only admin or assigned company representative can view company representatives");
        }
    }

    private void assertCanManageRepresentatives(UUID companyId, UUID actingUserId) {
        User actingUser = authRepository.findById(actingUserId).orElseThrow(() -> UserException.notFound("User not found"));

        if (actingUser.getRole() == Role.ADMIN) {
            return;
        }

        if (actingUser.getRole() != Role.COMPANY_REPRESENTATIVE || actingUser.getAccountStatus() != AccountStatus.ACTIVE) {
            throw CompanyException.forbidden("Only admin or primary contact can manage company representatives");
        }

        CompanyRepresentativeAssignment actingAssignment = companyRepresentativeAssignmentRepository
                .findByCompanyIdAndUserId(companyId, actingUserId)
                .filter(assignment -> assignment.getStatus() == CompanyRepresentativeAssignmentStatus.ACTIVE)
                .orElseThrow(() -> CompanyException.forbidden("Only admin or primary contact can manage company representatives"));

        if (actingAssignment.getAssignmentRole() != CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT) {
            throw CompanyException.forbidden("Only admin or primary contact can manage company representatives");
        }
    }

    private CompanyRepresentativeAssignmentRole resolveAssignmentRoleForNewActiveAssignment(UUID companyId) {
        boolean activePrimaryContactExists = companyRepresentativeAssignmentRepository
                .findByCompanyIdAndStatusAndAssignmentRole(
                        companyId,
                        CompanyRepresentativeAssignmentStatus.ACTIVE,
                        CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT
                )
                .isPresent();

        return activePrimaryContactExists ? CompanyRepresentativeAssignmentRole.MANAGER : CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT;
    }

    private CompanyRepresentativeAssignmentResponse toResponse(CompanyRepresentativeAssignment assignment) {
        return new CompanyRepresentativeAssignmentResponse(
                assignment.getId(),
                assignment.getCompanyId(),
                assignment.getUserId(),
                assignment.getAssignedByUserId(),
                assignment.getAssignmentRole(),
                assignment.getStatus(),
                assignment.getRevokedAt(),
                assignment.getRevokedByUserId()
        );
    }
}