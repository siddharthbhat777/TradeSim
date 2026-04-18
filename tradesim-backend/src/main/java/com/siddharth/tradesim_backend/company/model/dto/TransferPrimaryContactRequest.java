package com.siddharth.tradesim_backend.company.model.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TransferPrimaryContactRequest(
        @NotNull(message = "New primary contact user id is required")
        UUID newPrimaryContactUserId
) {
}