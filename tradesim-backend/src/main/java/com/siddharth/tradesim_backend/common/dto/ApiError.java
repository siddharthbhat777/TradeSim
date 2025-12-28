package com.siddharth.tradesim_backend.common.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class ApiError {
    private String message;
    private String errorCode;
    private String path;

    @Builder.Default
    private Instant timestamp = Instant.now();
}