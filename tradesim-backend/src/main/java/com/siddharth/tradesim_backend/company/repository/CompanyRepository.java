package com.siddharth.tradesim_backend.company.repository;

import com.siddharth.tradesim_backend.company.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    boolean existsByName(String name);
    boolean existsByCode(String code);
    // Optional<Company> findByCode(String code);
}