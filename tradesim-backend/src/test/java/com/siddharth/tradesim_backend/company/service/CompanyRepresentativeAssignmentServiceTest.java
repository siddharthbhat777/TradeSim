package com.siddharth.tradesim_backend.company.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentStatus;
import com.siddharth.tradesim_backend.company.enums.CompanyStatus;
import com.siddharth.tradesim_backend.company.model.Company;
import com.siddharth.tradesim_backend.company.model.CompanyRepresentativeAssignment;
import com.siddharth.tradesim_backend.company.model.dto.CompanyRepresentativeAssignmentResponse;
import com.siddharth.tradesim_backend.company.repository.CompanyRepresentativeAssignmentRepository;
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
class CompanyRepresentativeAssignmentServiceTest {

    @Mock
    private CompanyRepresentativeAssignmentRepository companyRepresentativeAssignmentRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AuthRepository authRepository;

    @InjectMocks
    private CompanyRepresentativeAssignmentService companyRepresentativeAssignmentService;

    @Test
    void shouldAssignCompanyRepresentativeWhenUserHasCompanyRepresentativeRole() {
        UUID companyId = UUID.randomUUID();
        UUID representativeUserId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        Company company = Company.builder()
                .id(companyId)
                .name("Apple Inc")
                .code("APPLE")
                .country("United States")
                .status(CompanyStatus.ACTIVE)
                .build();

        User representative = User.builder()
                .id(representativeUserId)
                .username("representative1")
                .email("representative1@example.com")
                .password("encoded")
                .role(Role.COMPANY_REPRESENTATIVE)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(authRepository.findById(representativeUserId)).thenReturn(Optional.of(representative));
        when(companyRepresentativeAssignmentRepository.findByCompanyIdAndUserId(companyId, representativeUserId)).thenReturn(Optional.empty());
        when(companyRepresentativeAssignmentRepository.save(any(CompanyRepresentativeAssignment.class))).thenAnswer(invocation -> {
            CompanyRepresentativeAssignment assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID());
            return assignment;
        });

        CompanyRepresentativeAssignmentResponse response = companyRepresentativeAssignmentService.assignRepresentative(companyId, representativeUserId, adminUserId);

        assertThat(response.companyId()).isEqualTo(companyId);
        assertThat(response.userId()).isEqualTo(representativeUserId);
        assertThat(response.assignedByAdminId()).isEqualTo(adminUserId);
        assertThat(response.status()).isEqualTo(CompanyRepresentativeAssignmentStatus.ACTIVE);
    }

    @Test
    void shouldRejectAssignmentWhenUserIsNotCompanyRepresentative() {
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

        BusinessException exception = assertThrows(BusinessException.class, () -> companyRepresentativeAssignmentService.assignRepresentative(companyId, userId, adminUserId));

        assertThat(exception.getMessage()).isEqualTo("User is not a company representative");
        verify(companyRepresentativeAssignmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateActiveAssignment() {
        UUID companyId = UUID.randomUUID();
        UUID representativeUserId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        Company company = Company.builder()
                .id(companyId)
                .build();

        User representative = User.builder()
                .id(representativeUserId)
                .role(Role.COMPANY_REPRESENTATIVE)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        CompanyRepresentativeAssignment existingAssignment = CompanyRepresentativeAssignment.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .userId(representativeUserId)
                .assignedByAdminId(adminUserId)
                .status(CompanyRepresentativeAssignmentStatus.ACTIVE)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(authRepository.findById(representativeUserId)).thenReturn(Optional.of(representative));
        when(companyRepresentativeAssignmentRepository.findByCompanyIdAndUserId(companyId, representativeUserId)).thenReturn(Optional.of(existingAssignment));

        BusinessException exception = assertThrows(BusinessException.class, () -> companyRepresentativeAssignmentService.assignRepresentative(companyId, representativeUserId, adminUserId));

        assertThat(exception.getMessage()).isEqualTo("Company representative is already assigned to this company");
        verify(companyRepresentativeAssignmentRepository, never()).save(any());
    }

    @Test
    void shouldRevokeRepresentativeAssignment() {
        UUID companyId = UUID.randomUUID();
        UUID representativeUserId = UUID.randomUUID();

        CompanyRepresentativeAssignment assignment = CompanyRepresentativeAssignment.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .userId(representativeUserId)
                .assignedByAdminId(UUID.randomUUID())
                .status(CompanyRepresentativeAssignmentStatus.ACTIVE)
                .build();

        when(companyRepresentativeAssignmentRepository.findByCompanyIdAndUserId(companyId, representativeUserId)).thenReturn(Optional.of(assignment));
        when(companyRepresentativeAssignmentRepository.save(any(CompanyRepresentativeAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompanyRepresentativeAssignmentResponse response = companyRepresentativeAssignmentService.revokeRepresentative(companyId, representativeUserId);

        assertThat(response.status()).isEqualTo(CompanyRepresentativeAssignmentStatus.REVOKED);
        assertThat(response.revokedAt()).isNotNull();
    }

    @Test
    void shouldFetchOnlyActiveAssignments() {
        UUID companyId = UUID.randomUUID();

        CompanyRepresentativeAssignment assignment = CompanyRepresentativeAssignment.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .userId(UUID.randomUUID())
                .assignedByAdminId(UUID.randomUUID())
                .status(CompanyRepresentativeAssignmentStatus.ACTIVE)
                .revokedAt(null)
                .build();

        when(companyRepository.existsById(companyId)).thenReturn(true);
        when(companyRepresentativeAssignmentRepository.findByCompanyIdAndStatus(companyId, CompanyRepresentativeAssignmentStatus.ACTIVE)).thenReturn(List.of(assignment));

        List<CompanyRepresentativeAssignmentResponse> responses = companyRepresentativeAssignmentService.fetchActiveAssignments(companyId);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().status()).isEqualTo(CompanyRepresentativeAssignmentStatus.ACTIVE);
        assertThat(responses.getFirst().revokedAt()).isNull();
    }
}