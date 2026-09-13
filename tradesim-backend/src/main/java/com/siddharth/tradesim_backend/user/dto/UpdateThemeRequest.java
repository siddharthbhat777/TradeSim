package com.siddharth.tradesim_backend.user.dto;

import com.siddharth.tradesim_backend.auth.enums.ThemePreference;
import jakarta.validation.constraints.NotNull;

public record UpdateThemeRequest(
        @NotNull(message = "Theme preference is required")
        ThemePreference theme
) {
}