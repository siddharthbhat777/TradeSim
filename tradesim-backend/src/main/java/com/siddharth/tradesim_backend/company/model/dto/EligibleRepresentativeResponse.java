package com.siddharth.tradesim_backend.company.model.dto;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;

import java.util.UUID;

public record EligibleRepresentativeResponse(
        UUID id,
        String fullName,
        String username,
        String email,
        AccountStatus accountStatus
) {
}