package com.siddharth.tradesim_backend.company.model.dto;

import com.siddharth.tradesim_backend.company.enums.CompanyStatus;

import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        String code,
        String country,
        CompanyStatus status
) {
}