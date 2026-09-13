package com.siddharth.tradesim_backend.listing.model.dto;

import java.util.UUID;

public record CapTableEntryResponse(
        UUID userId,
        int quantity
) {
}