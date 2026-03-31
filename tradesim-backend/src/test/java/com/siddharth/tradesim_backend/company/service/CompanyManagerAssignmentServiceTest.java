package com.siddharth.tradesim_backend.company.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.company.enums.CompanyManagerAssignmentStatus;
import com.siddharth.tradesim_backend.company.enums.CompanyStatus;
import com.siddharth.tradesim_backend.company.model.Company;
import com.siddharth.tradesim_backend.company.model.CompanyManagerAssignment;
import com.siddharth.tradesim_backend.company.model.dto.CompanyManagerAssignmentResponse;
import com.siddharth.tradesim_backend.company.repository.CompanyManagerAssignmentRepository;
import com.siddharth.tradesim_backend.company.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyManagerAssignmentServiceTest {

    @Mock
    private CompanyManagerAssignmentRepository companyManagerAssignmentRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AuthRepository authRepository;

    @InjectMocks
    private CompanyManagerAssignmentService companyManagerAssignmentService;

    @Test
    void shouldAssignCompanyManagerWhenUserHasCompanyManagerRole() {
        UUID companyId = UUID.randomUUID();
        UUID managerUserId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        Company company = Company.builder()
                .id(companyId)
                .name("Apple Inc")
                .code("APPLE")
                .country("United States")
                .status(CompanyStatus.ACTIVE)
                .build();

        User manager = User.builder()
                .id(managerUserId)
                .username("manager1")
                .email("manager1@example.com")
                .password("encoded")
                .role(Role.COMPANY_MANAGER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(authRepository.findById(managerUserId)).thenReturn(Optional.of(manager));
        when(companyManagerAssignmentRepository.findByCompanyIdAndUserId(companyId, managerUserId)).thenReturn(Optional.empty());
        when(companyManagerAssignmentRepository.save(any(CompanyManagerAssignment.class))).thenAnswer(invocation -> {
            CompanyManagerAssignment assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID());
            return assignment;
        });

        CompanyManagerAssignmentResponse response = companyManagerAssignmentService.assignManager(companyId, managerUserId, adminUserId);

        assertThat(response.companyId()).isEqualTo(companyId);
        assertThat(response.userId()).isEqualTo(managerUserId);
        assertThat(response.assignedByAdminId()).isEqualTo(adminUserId);
        assertThat(response.status()).isEqualTo(CompanyManagerAssignmentStatus.ACTIVE);
    }

    @Test
    void shouldRejectAssignmentWhenUserIsNotCompanyManager() {
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        Company company = Company.builder()
                .id(companyId)
                .build();

        User user = User.builder()
                .id(userId)
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(authRepository.findById(userId)).thenReturn(Optional.of(user));

        BusinessException exception = assertThrows(BusinessException.class, () -> companyManagerAssignmentService.assignManager(companyId, userId, adminUserId));

        assertThat(exception.getMessage()).isEqualTo("User is not a company manager");
        verify(companyManagerAssignmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateActiveAssignment() {
        UUID companyId = UUID.randomUUID();
        UUID managerUserId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        Company company = Company.builder()
                .id(companyId)
                .build();

        User manager = User.builder()
                .id(managerUserId)
                .role(Role.COMPANY_MANAGER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        CompanyManagerAssignment existingAssignment = CompanyManagerAssignment.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .userId(managerUserId)
                .assignedByAdminId(adminUserId)
                .status(CompanyManagerAssignmentStatus.ACTIVE)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(authRepository.findById(managerUserId)).thenReturn(Optional.of(manager));
        when(companyManagerAssignmentRepository.findByCompanyIdAndUserId(companyId, managerUserId)).thenReturn(Optional.of(existingAssignment));

        BusinessException exception = assertThrows(BusinessException.class, () -> companyManagerAssignmentService.assignManager(companyId, managerUserId, adminUserId));

        assertThat(exception.getMessage()).isEqualTo("Company manager is already assigned to this company");
        verify(companyManagerAssignmentRepository, never()).save(any());
    }

    @Test
    void shouldRevokeManagerAssignment() {
        UUID companyId = UUID.randomUUID();
        UUID managerUserId = UUID.randomUUID();

        CompanyManagerAssignment assignment = CompanyManagerAssignment.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .userId(managerUserId)
                .assignedByAdminId(UUID.randomUUID())
                .status(CompanyManagerAssignmentStatus.ACTIVE)
                .build();

        when(companyManagerAssignmentRepository.findByCompanyIdAndUserId(companyId, managerUserId)).thenReturn(Optional.of(assignment));
        when(companyManagerAssignmentRepository.save(any(CompanyManagerAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompanyManagerAssignmentResponse response = companyManagerAssignmentService.revokeManager(companyId, managerUserId);

        assertThat(response.status()).isEqualTo(CompanyManagerAssignmentStatus.REVOKED);
        assertThat(response.revokedAt()).isNotNull();
    }

    @Test
    void shouldFetchOnlyActiveAssignments() {
        UUID companyId = UUID.randomUUID();

        CompanyManagerAssignment assignment = CompanyManagerAssignment.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .userId(UUID.randomUUID())
                .assignedByAdminId(UUID.randomUUID())
                .status(CompanyManagerAssignmentStatus.ACTIVE)
                .revokedAt(null)
                .build();

        when(companyRepository.existsById(companyId)).thenReturn(true);
        when(companyManagerAssignmentRepository.findByCompanyIdAndStatus(companyId, CompanyManagerAssignmentStatus.ACTIVE)).thenReturn(List.of(assignment));

        List<CompanyManagerAssignmentResponse> responses = companyManagerAssignmentService.fetchActiveAssignments(companyId);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().status()).isEqualTo(CompanyManagerAssignmentStatus.ACTIVE);
        assertThat(responses.getFirst().revokedAt()).isNull();
    }
}