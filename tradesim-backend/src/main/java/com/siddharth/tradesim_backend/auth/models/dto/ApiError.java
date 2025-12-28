package com.siddharth.tradesim_backend.auth.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiError {
    private String message;
    private String errorCode;
    private String path;

    @Builder.Default
    private Instant timestamp = Instant.now();
}