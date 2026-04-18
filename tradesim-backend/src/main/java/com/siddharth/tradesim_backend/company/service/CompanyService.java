package com.siddharth.tradesim_backend.company.service;

import com.siddharth.tradesim_backend.company.CompanyException;
import com.siddharth.tradesim_backend.company.enums.CompanyStatus;
import com.siddharth.tradesim_backend.company.model.Company;
import com.siddharth.tradesim_backend.company.model.dto.CompanyResponse;
import com.siddharth.tradesim_backend.company.model.dto.CreateCompanyRequest;
import com.siddharth.tradesim_backend.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public List<CompanyResponse> fetchCompanies() {
        return companyRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CompanyResponse fetchCompany(UUID companyId) {
        Company company = companyRepository.findById(companyId).orElseThrow(() -> CompanyException.notFound("Company not found"));
        return toResponse(company);
    }

    @Transactional
    public CompanyResponse createCompany(CreateCompanyRequest request) {
        if (companyRepository.existsByName(request.name())) {
            throw CompanyException.conflict("Company with this name already exists");
        }

        if (companyRepository.existsByCode(request.code())) {
            throw CompanyException.conflict("Company with this code already exists");
        }

        try {
            Company company = Company.builder()
                    .name(request.name())
                    .code(request.code())
                    .country(request.country())
                    .status(CompanyStatus.ACTIVE)
                    .build();

            Company saved = companyRepository.save(company);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw CompanyException.badRequest("Invalid company data");
        }
    }

    @Transactional
    public CompanyResponse changeStatus(UUID companyId, CompanyStatus status) {
        Company company = companyRepository.findById(companyId).orElseThrow(() -> CompanyException.notFound("Company not found"));

        if (company.getStatus() == status) {
            throw CompanyException.conflict("Company already has this status");
        }

        company.setStatus(status);
        Company saved = companyRepository.save(company);
        return toResponse(saved);
    }

    private CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getCode(),
                company.getCountry(),
                company.getStatus()
        );
    }
}