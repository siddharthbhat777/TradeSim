package com.siddharth.tradesim_backend.auth.service;

import com.siddharth.tradesim_backend.auth.model.User;

public record RefreshTokenRotation(
        String refreshToken,
        User user
) {
}