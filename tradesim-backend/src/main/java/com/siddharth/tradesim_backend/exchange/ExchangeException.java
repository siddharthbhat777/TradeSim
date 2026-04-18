package com.siddharth.tradesim_backend.exchange;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class ExchangeException extends BusinessException {
    private ExchangeException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static ExchangeException notFound(String message) {
        return new ExchangeException(HttpStatus.NOT_FOUND, "EXCHANGE_NOT_FOUND", message);
    }

    public static ExchangeException conflict(String message) {
        return new ExchangeException(HttpStatus.CONFLICT, "EXCHANGE_CONFLICT", message);
    }

    public static ExchangeException badRequest(String message) {
        return new ExchangeException(HttpStatus.BAD_REQUEST, "EXCHANGE_INVALID_REQUEST", message);
    }
}