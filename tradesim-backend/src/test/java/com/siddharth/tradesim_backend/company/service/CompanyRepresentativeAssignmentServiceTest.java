package com.siddharth.tradesim_backend.company.service;

import com.siddharth.tradesim_backend.auth.repository.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentRole;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentStatus;
import com.siddharth.tradesim_backend.company.model.CompanyRepresentativeAssignment;
import com.siddharth.tradesim_backend.company.model.dto.CompanyRepresentativeAssignmentResponse;
import com.siddharth.tradesim_backend.company.model.dto.PrimaryContactTransferResponse;
import com.siddharth.tradesim_backend.company.repository.CompanyRepository;
import com.siddharth.tradesim_backend.company.repository.CompanyRepresentativeAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    void shouldAssignFirstRepresentativeAsPrimaryContactWhenAdminActs() {
        UUID companyId = UUID.randomUUID();
        UUID representativeUserId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        User admin = User.builder().id(adminUserId).role(Role.ADMIN).accountStatus(AccountStatus.ACTIVE).build();
        User representative = User.builder().id(representativeUserId).role(Role.COMPANY_REPRESENTATIVE).accountStatus(AccountStatus.ACTIVE).build();

        when(companyRepository.existsById(companyId)).thenReturn(true);
        when(authRepository.findById(adminUserId)).thenReturn(Optional.of(admin));
        when(authRepository.findById(representativeUserId)).thenReturn(Optional.of(representative));
        when(companyRepresentativeAssignmentRepository.findByCompanyIdAndUserId(companyId, representativeUserId)).thenReturn(Optional.empty());
        when(companyRepresentativeAssignmentRepository.findByCompanyIdAndStatusAndAssignmentRole(companyId, CompanyRepresentativeAssignmentStatus.ACTIVE, CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT)).thenReturn(Optional.empty());
        when(companyRepresentativeAssignmentRepository.save(any(CompanyRepresentativeAssignment.class))).thenAnswer(invocation -> {
            CompanyRepresentativeAssignment assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID());
            return assignment;
        });

        CompanyRepresentativeAssignmentResponse response = companyRepresentativeAssignmentService.assignRepresentative(companyId, representativeUserId, adminUserId);

        assertThat(response.assignedByUserId()).isEqualTo(adminUserId);
        assertThat(response.assignmentRole()).isEqualTo(CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT);
        assertThat(response.status()).isEqualTo(CompanyRepresentativeAssignmentStatus.ACTIVE);
    }

    @Test
    void shouldAssignAdditionalRepresentativeAsManagerWhenPrimaryContactExists() {
        UUID companyId = UUID.randomUUID();
        UUID primaryContactUserId = UUID.randomUUID();
        UUID representativeUserId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        User admin = User.builder().id(adminUserId).role(Role.ADMIN).accountStatus(AccountStatus.ACTIVE).build();
        User representative = User.builder().id(representativeUserId).role(Role.COMPANY_REPRESENTATIVE).accountStatus(AccountStatus.ACTIVE).build();

        CompanyRepresentativeAssignment primaryContact = CompanyRepresentativeAssignment.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .userId(primaryContactUserId)
                .assignedByUserId(adminUserId)
                .assignmentRole(CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT)
                .status(CompanyRepresentativeAssignmentStatus.ACTIVE)
                .build();

        when(companyRepository.existsById(companyId)).thenReturn(true);
        when(authRepository.findById(adminUserId)).thenReturn(Optional.of(admin));
        when(authRepository.findById(representativeUserId)).thenReturn(Optional.of(representative));
        when(companyRepresentativeAssignmentRepository.findByCompanyIdAndUserId(companyId, representativeUserId)).thenReturn(Optional.empty());
        when(companyRepresentativeAssignmentRepository.findByCompanyIdAndStatusAndAssignmentRole(companyId, CompanyRepresentativeAssignmentStatus.ACTIVE, CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT)).thenReturn(Optional.of(primaryContact));
        when(companyRepresentativeAssignmentRepository.save(any(CompanyRepresentativeAssignment.class))).thenAnswer(invocation -> {
            CompanyRepresentativeAssignment assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID());
            return assignment;
        });

        CompanyRepresentativeAssignmentResponse response = companyRepresentativeAssignmentService.assignRepresentative(companyId, representativeUserId, adminUserId);

        assertThat(response.assignmentRole()).isEqualTo(CompanyRepresentativeAssignmentRole.MANAGER);
    }

    @Test
    void shouldRejectManagerTryingToAssignRepresentative() {
        UUID companyId = UUID.randomUUID();
        UUID managerUserId = UUID.randomUUID();
        UUID targetRepresentativeUserId = UUID.randomUUID();

        User managerUser = User.builder().id(managerUserId).role(Role.COMPANY_REPRESENTATIVE).accountStatus(AccountStatus.ACTIVE).build();
        User targetRepresentative = User.builder().id(targetRepresentativeUserId).role(Role.COMPANY_REPRESENTATIVE).accountStatus(AccountStatus.ACTIVE).build();

        CompanyRepresentativeAssignment managerAssignment = CompanyRepresentativeAssignment.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .userId(managerUserId)
                .assignedByUserId(UUID.randomUUID())
                .assignmentRole(CompanyRepresentativeAssignmentRole.MANAGER)
                .status(CompanyRepresentativeAssignmentStatus.ACTIVE)
                .build();

        when(companyRepository.existsById(companyId)).thenReturn(true);
        when(authRepository.findById(managerUserId)).thenReturn(Optional.of(managerUser));
        when(authRepository.findById(targetRepresentativeUserId)).thenReturn(Optional.of(targetRepresentative));
        when(companyRepresentativeAssignmentRepository.findByCompanyIdAndUserId(companyId, managerUserId)).thenReturn(Optional.of(managerAssignment));

        BusinessException exception = assertThrows(BusinessException.class, () -> companyRepresentativeAssignmentService.assignRepresentative(companyId, targetRepresentativeUserId, managerUserId));

        assertThat(exception.getMessage()).isEqualTo("Only admin or primary contact can manage company representatives");
        verify(companyRepresentativeAssignmentRepository, never()).save(any());
    }

    @Test
    void shouldTransferPrimaryContact() {
        UUID companyId = UUID.randomUUID();
        UUID currentPrimaryContactUserId = UUID.randomUUID();
        UUID newPrimaryContactUserId = UUID.randomUUID();

        User actingPrimaryContact = User.builder().id(currentPrimaryContactUserId).role(Role.COMPANY_REPRESENTATIVE).accountStatus(AccountStatus.ACTIVE).build();

        CompanyRepresentativeAssignment currentPrimaryContact = CompanyRepresentativeAssignment.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .userId(currentPrimaryContactUserId)
                .assignedByUserId(UUID.randomUUID())
                .assignmentRole(CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT)
                .status(CompanyRepresentativeAssignmentStatus.ACTIVE)
                .build();

        CompanyRepresentativeAssignment newPrimaryContact = CompanyRepresentativeAssignment.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .userId(newPrimaryContactUserId)
                .assignedByUserId(UUID.randomUUID())
                .assignmentRole(CompanyRepresentativeAssignmentRole.MANAGER)
                .status(CompanyRepresentativeAssignmentStatus.ACTIVE)
                .build();

        when(authRepository.findById(currentPrimaryContactUserId)).thenReturn(Optional.of(actingPrimaryContact));
        when(companyRepresentativeAssignmentRepository.findByCompanyIdAndUserId(companyId, currentPrimaryContactUserId)).thenReturn(Optional.of(currentPrimaryContact));
        when(companyRepresentativeAssignmentRepository.findByCompanyIdAndStatusAndAssignmentRole(companyId, CompanyRepresentativeAssignmentStatus.ACTIVE, CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT)).thenReturn(Optional.of(currentPrimaryContact));
        when(companyRepresentativeAssignmentRepository.findByCompanyIdAndUserId(companyId, newPrimaryContactUserId)).thenReturn(Optional.of(newPrimaryContact));
        when(companyRepresentativeAssignmentRepository.save(any(CompanyRepresentativeAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PrimaryContactTransferResponse response = companyRepresentativeAssignmentService.transferPrimaryContact(companyId, newPrimaryContactUserId, currentPrimaryContactUserId);

        assertThat(response.previousPrimaryContactUserId()).isEqualTo(currentPrimaryContactUserId);
        assertThat(response.newPrimaryContactUserId()).isEqualTo(newPrimaryContactUserId);
        assertThat(currentPrimaryContact.getAssignmentRole()).isEqualTo(CompanyRepresentativeAssignmentRole.MANAGER);
        assertThat(newPrimaryContact.getAssignmentRole()).isEqualTo(CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT);
    }

    @Test
    void shouldRejectRevokingCurrentPrimaryContactWithoutTransfer() {
        UUID companyId = UUID.randomUUID();
        UUID primaryContactUserId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        User admin = User.builder().id(adminUserId).role(Role.ADMIN).accountStatus(AccountStatus.ACTIVE).build();

        CompanyRepresentativeAssignment primaryContactAssignment = CompanyRepresentativeAssignment.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .userId(primaryContactUserId)
                .assignedByUserId(adminUserId)
                .assignmentRole(CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT)
                .status(CompanyRepresentativeAssignmentStatus.ACTIVE)
                .build();

        when(authRepository.findById(adminUserId)).thenReturn(Optional.of(admin));
        when(companyRepresentativeAssignmentRepository.findByCompanyIdAndUserId(companyId, primaryContactUserId)).thenReturn(Optional.of(primaryContactAssignment));

        BusinessException exception = assertThrows(BusinessException.class, () -> companyRepresentativeAssignmentService.revokeRepresentative(companyId, primaryContactUserId, adminUserId));

        assertThat(exception.getMessage()).isEqualTo("Transfer primary contact before revoking the current primary contact");
    }

    @Test
    void shouldAllowPrimaryContactWhenIssuanceRequiresPrimaryContact() {
        UUID companyId = UUID.randomUUID();
        UUID primaryContactUserId = UUID.randomUUID();

        User primaryContactUser = User.builder()
                .id(primaryContactUserId)
                .role(Role.COMPANY_REPRESENTATIVE)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        CompanyRepresentativeAssignment primaryContactAssignment = CompanyRepresentativeAssignment.builder()
                .companyId(companyId)
                .userId(primaryContactUserId)
                .assignmentRole(CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT)
                .status(CompanyRepresentativeAssignmentStatus.ACTIVE)
                .build();

        when(authRepository.findById(primaryContactUserId)).thenReturn(Optional.of(primaryContactUser));
        when(companyRepresentativeAssignmentRepository.findByCompanyIdAndUserId(companyId, primaryContactUserId)).thenReturn(Optional.of(primaryContactAssignment));

        companyRepresentativeAssignmentService.assertPrimaryContactAssignment(companyId, primaryContactUserId);
    }

    @Test
    void shouldRejectManagerWhenIssuanceRequiresPrimaryContact() {
        UUID companyId = UUID.randomUUID();
        UUID managerUserId = UUID.randomUUID();

        User managerUser = User.builder()
                .id(managerUserId)
                .role(Role.COMPANY_REPRESENTATIVE)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        CompanyRepresentativeAssignment managerAssignment = CompanyRepresentativeAssignment.builder()
                .companyId(companyId)
                .userId(managerUserId)
                .assignmentRole(CompanyRepresentativeAssignmentRole.MANAGER)
                .status(CompanyRepresentativeAssignmentStatus.ACTIVE)
                .build();

        when(authRepository.findById(managerUserId)).thenReturn(Optional.of(managerUser));
        when(companyRepresentativeAssignmentRepository.findByCompanyIdAndUserId(companyId, managerUserId)).thenReturn(Optional.of(managerAssignment));

        BusinessException exception = assertThrows(BusinessException.class, () -> companyRepresentativeAssignmentService.assertPrimaryContactAssignment(companyId, managerUserId));

        assertThat(exception.getMessage()).isEqualTo("Only an active primary contact can perform this company action");
    }
}