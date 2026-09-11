package com.siddharth.tradesim_backend.user.dto;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.enums.ThemePreference;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String fullName,
        String username,
        String email,
        String linkedBankName,
        Role role,
        AccountStatus accountStatus,
        ThemePreference themePreference,
        String countryCode,
        Instant lastLogin
) {
}