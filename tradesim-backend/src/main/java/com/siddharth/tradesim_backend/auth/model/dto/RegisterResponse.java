package com.siddharth.tradesim_backend.auth.model.dto;

import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;

import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String username,
        String email,
        Role role,
        AccountStatus accountStatus
) {
}