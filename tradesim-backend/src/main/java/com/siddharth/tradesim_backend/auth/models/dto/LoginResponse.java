package com.siddharth.tradesim_backend.auth.models.dto;

import com.siddharth.tradesim_backend.auth.enums.Role;

public record LoginResponse(
        String token,
        String username,
        Role role
) {
}