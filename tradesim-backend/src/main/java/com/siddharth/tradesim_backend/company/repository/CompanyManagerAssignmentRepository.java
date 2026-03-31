package com.siddharth.tradesim_backend.company.repository;

import com.siddharth.tradesim_backend.company.enums.CompanyManagerAssignmentStatus;
import com.siddharth.tradesim_backend.company.model.CompanyManagerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyManagerAssignmentRepository extends JpaRepository<CompanyManagerAssignment, UUID> {
    Optional<CompanyManagerAssignment> findByCompanyIdAndUserId(UUID companyId, UUID userId);
    List<CompanyManagerAssignment> findByCompanyIdAndStatus(UUID companyId, CompanyManagerAssignmentStatus status);
}