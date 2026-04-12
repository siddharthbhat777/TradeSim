package com.siddharth.tradesim_backend.ipo;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class IpoException extends BusinessException {
    private IpoException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static IpoException notFound(String message) {
        return new IpoException(HttpStatus.NOT_FOUND, "IPO_NOT_FOUND", message);
    }

    public static IpoException conflict(String message) {
        return new IpoException(HttpStatus.CONFLICT, "IPO_CONFLICT", message);
    }

    public static IpoException badRequest(String message) {
        return new IpoException(HttpStatus.BAD_REQUEST, "IPO_INVALID_REQUEST", message);
    }

    public static IpoException forbidden(String message) {
        return new IpoException(HttpStatus.FORBIDDEN, "IPO_FORBIDDEN", message);
    }
}