package com.siddharth.tradesim_backend.auth.model.dto;

import com.siddharth.tradesim_backend.auth.enums.Role;

public record AuthTokenResult(
        String accessToken,
        String refreshToken,
        String username,
        Role role
) {
}