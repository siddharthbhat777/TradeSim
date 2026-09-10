package com.siddharth.tradesim_backend.auth.model.dto;

import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;

import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String fullName,
        String username,
        String email,
        String linkedBankName,
        Role role,
        AccountStatus accountStatus
) {
}