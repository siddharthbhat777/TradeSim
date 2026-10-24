package com.siddharth.tradesim_backend.notification.model.dto;

import com.siddharth.tradesim_backend.notification.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String message,
        String referenceId,
        NotificationType type,
        boolean isRead,
        Instant createdAt
) {
}