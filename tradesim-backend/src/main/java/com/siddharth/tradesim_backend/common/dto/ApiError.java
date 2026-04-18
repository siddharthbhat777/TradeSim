package com.siddharth.tradesim_backend.common.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class ApiError {
    private final int status;
    private final String error;
    private final String errorCode;
    private final String message;
    private final String path;

    @Builder.Default
    private final Instant timestamp = Instant.now();

    @Builder.Default
    private final Map<String, String> fieldErrors = Map.of();
}