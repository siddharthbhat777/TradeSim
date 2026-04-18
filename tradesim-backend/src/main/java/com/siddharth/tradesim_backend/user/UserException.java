package com.siddharth.tradesim_backend.user;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class UserException extends BusinessException {
    protected UserException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static UserException notFound(String message) {
        return new UserException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", message);
    }

    public static UserException conflict(String message) {
        return new UserException(HttpStatus.CONFLICT, "USER_CONFLICT", message);
    }

    public static UserException badRequest(String message) {
        return new UserException(HttpStatus.BAD_REQUEST, "USER_INVALID_REQUEST", message);
    }

    public static UserException forbidden(String message) {
        return new UserException(HttpStatus.FORBIDDEN, "USER_FORBIDDEN", message);
    }
}