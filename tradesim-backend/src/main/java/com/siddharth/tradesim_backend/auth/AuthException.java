package com.siddharth.tradesim_backend.auth;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class AuthException extends BusinessException {
    protected AuthException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static AuthException unauthorized(String message) {
        return new AuthException(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", message);
    }

    public static AuthException badRequest(String message) {
        return new AuthException(HttpStatus.BAD_REQUEST, "AUTH_INVALID_REQUEST", message);
    }

    public static AuthException conflict(String message) {
        return new AuthException(HttpStatus.CONFLICT, "AUTH_CONFLICT", message);
    }

    public static AuthException forbidden(String message) {
        return new AuthException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", message);
    }
}