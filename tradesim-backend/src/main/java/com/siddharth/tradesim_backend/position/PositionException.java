package com.siddharth.tradesim_backend.position;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class PositionException extends BusinessException {
    private PositionException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static PositionException notFound(String message) {
        return new PositionException(HttpStatus.NOT_FOUND, "POSITION_NOT_FOUND", message);
    }

    public static PositionException conflict(String message) {
        return new PositionException(HttpStatus.CONFLICT, "POSITION_CONFLICT", message);
    }

    public static PositionException badRequest(String message) {
        return new PositionException(HttpStatus.BAD_REQUEST, "POSITION_INVALID_REQUEST", message);
    }
}