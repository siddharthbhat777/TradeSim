package com.siddharth.tradesim_backend.common.error;

import com.siddharth.tradesim_backend.common.dto.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ApiErrorResponseWriter {
    private final ObjectMapper objectMapper;

    public ApiError build(HttpStatus status, String errorCode, String message, String path) {
        return build(status, errorCode, message, path, Map.of());
    }

    public ApiError build(HttpStatus status, String errorCode, String message, String path, Map<String, String> fieldErrors) {
        return ApiError.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .fieldErrors(fieldErrors == null ? Map.of() : fieldErrors)
                .build();
    }

    public void write(HttpServletResponse response, HttpStatus status, String errorCode, String message, String path) throws IOException {
        write(response, build(status, errorCode, message, path));
    }

    public void write(HttpServletResponse response, ApiError error) throws IOException {
        response.setStatus(error.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), error);
    }
}