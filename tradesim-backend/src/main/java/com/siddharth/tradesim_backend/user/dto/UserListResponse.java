package com.siddharth.tradesim_backend.user.dto;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record UserListResponse(
        UUID id,
        String fullName,
        String username,
        String email,
        Role role,
        AccountStatus accountStatus,
        String countryCode,
        Instant lastLogin,
        Instant createdAt
) {
}