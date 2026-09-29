package com.siddharth.tradesim_backend.company.service;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentRole;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentStatus;
import com.siddharth.tradesim_backend.company.enums.CompanyStatus;
import com.siddharth.tradesim_backend.company.model.Company;
import com.siddharth.tradesim_backend.company.model.CompanyRepresentativeAssignment;
import com.siddharth.tradesim_backend.company.model.dto.CompanyResponse;
import com.siddharth.tradesim_backend.company.model.dto.CreateCompanyRequest;
import com.siddharth.tradesim_backend.company.repository.CompanyRepository;
import com.siddharth.tradesim_backend.company.repository.CompanyRepresentativeAssignmentRepository;
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
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyRepresentativeAssignmentRepository companyRepresentativeAssignmentRepository;

    @InjectMocks
    private CompanyService companyService;

    @Test
    void shouldFetchAllCompanies() {
        Company company = Company.builder()
                .id(UUID.randomUUID())
                .name("Apple Inc")
                .code("APPLE")
                .country("United States")
                .status(CompanyStatus.ACTIVE)
                .build();

        when(companyRepository.findAll()).thenReturn(List.of(company));
        when(companyRepresentativeAssignmentRepository.findByCompanyIdAndStatusAndAssignmentRole(
                company.getId(), CompanyRepresentativeAssignmentStatus.ACTIVE, CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT))
                .thenReturn(Optional.empty());

        List<CompanyResponse> responses = companyService.fetchCompanies();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().code()).isEqualTo("APPLE");
        assertThat(responses.getFirst().country()).isEqualTo("United States");
        assertThat(responses.getFirst().primaryContactId()).isNull();
    }

    @Test
    void shouldFetchAssignedCompaniesForRepresentative() {
        UUID repId = UUID.randomUUID();
        Company company = Company.builder()
                .id(UUID.randomUUID())
                .name("Apple Inc")
                .code("APPLE")
                .country("United States")
                .status(CompanyStatus.ACTIVE)
                .build();

        CompanyRepresentativeAssignment assignment = CompanyRepresentativeAssignment.builder()
                .companyId(company.getId())
                .userId(repId)
                .build();

        when(companyRepresentativeAssignmentRepository.findByUserIdAndStatus(repId, CompanyRepresentativeAssignmentStatus.ACTIVE))
                .thenReturn(List.of(assignment));
        when(companyRepository.findAllById(List.of(company.getId()))).thenReturn(List.of(company));
        when(companyRepresentativeAssignmentRepository.findByCompanyIdAndStatusAndAssignmentRole(
                company.getId(), CompanyRepresentativeAssignmentStatus.ACTIVE, CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT))
                .thenReturn(Optional.of(assignment));

        List<CompanyResponse> responses = companyService.fetchAssignedCompanies(repId);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().code()).isEqualTo("APPLE");
        assertThat(responses.getFirst().primaryContactId()).isEqualTo(repId);
    }

    @Test
    void shouldCreateCompanyWhenRequestIsValid() {
        UUID companyId = UUID.randomUUID();
        CreateCompanyRequest request = new CreateCompanyRequest(
                "Apple Inc",
                "APPLE",
                "United States"
        );

        when(companyRepository.existsByName(request.name())).thenReturn(false);
        when(companyRepository.existsByCode(request.code())).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            company.setId(companyId);
            return company;
        });
        when(companyRepresentativeAssignmentRepository.findByCompanyIdAndStatusAndAssignmentRole(
                companyId, CompanyRepresentativeAssignmentStatus.ACTIVE, CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT))
                .thenReturn(Optional.empty());

        CompanyResponse response = companyService.createCompany(request);

        assertThat(response.id()).isEqualTo(companyId);
        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.code()).isEqualTo(request.code());
        assertThat(response.status()).isEqualTo(CompanyStatus.ACTIVE);
    }

    @Test
    void shouldChangeCompanyStatusWhenValid() {
        UUID companyId = UUID.randomUUID();
        Company company = Company.builder()
                .id(companyId)
                .name("Apple Inc")
                .code("APPLE")
                .country("United States")
                .status(CompanyStatus.ACTIVE)
                .build();

        UUID primaryContactId = UUID.randomUUID();
        CompanyRepresentativeAssignment assignment = CompanyRepresentativeAssignment.builder().userId(primaryContactId).build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyRepresentativeAssignmentRepository.findByCompanyIdAndStatusAndAssignmentRole(
                companyId, CompanyRepresentativeAssignmentStatus.ACTIVE, CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT))
                .thenReturn(Optional.of(assignment));

        CompanyResponse response = companyService.changeStatus(companyId, CompanyStatus.INACTIVE);

        assertThat(response.status()).isEqualTo(CompanyStatus.INACTIVE);
        assertThat(response.primaryContactId()).isEqualTo(primaryContactId);
    }

    @Test
    void shouldThrowExceptionWhenCompanyAlreadyHasRequestedStatus() {
        UUID companyId = UUID.randomUUID();
        Company company = Company.builder()
                .id(companyId)
                .status(CompanyStatus.ACTIVE)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        BusinessException exception = assertThrows(BusinessException.class, () -> companyService.changeStatus(companyId, CompanyStatus.ACTIVE));

        assertThat(exception.getMessage()).isEqualTo("Company already has this status");
        verify(companyRepository, never()).save(any());
    }
}