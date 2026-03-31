package com.siddharth.tradesim_backend.user.dto;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;

import java.util.UUID;

public record ChangeUserRoleResponse(
        UUID userId,
        String username,
        String email,
        Role role,
        AccountStatus accountStatus
) {
}