package com.siddharth.tradesim_backend.company.repository;

import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentStatus;
import com.siddharth.tradesim_backend.company.model.CompanyRepresentativeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepresentativeAssignmentRepository extends JpaRepository<CompanyRepresentativeAssignment, UUID> {
    Optional<CompanyRepresentativeAssignment> findByCompanyIdAndUserId(UUID companyId, UUID userId);
    List<CompanyRepresentativeAssignment> findByCompanyIdAndStatus(UUID companyId, CompanyRepresentativeAssignmentStatus status);
    boolean existsByUserIdAndStatus(UUID userId, CompanyRepresentativeAssignmentStatus status);
}