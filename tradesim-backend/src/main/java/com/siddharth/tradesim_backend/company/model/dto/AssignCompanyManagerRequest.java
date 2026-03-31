package com.siddharth.tradesim_backend.company.model.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignCompanyManagerRequest(
        @NotNull(message = "User id is required")
        UUID userId
) {
}