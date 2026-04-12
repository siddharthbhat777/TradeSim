package com.siddharth.tradesim_backend.stock;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class StockException extends BusinessException {
    protected StockException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static StockException notFound(String message) {
        return new StockException(HttpStatus.NOT_FOUND, "STOCK_NOT_FOUND", message);
    }

    public static StockException conflict(String message) {
        return new StockException(HttpStatus.CONFLICT, "STOCK_CONFLICT", message);
    }

    public static StockException badRequest(String message) {
        return new StockException(HttpStatus.BAD_REQUEST, "STOCK_INVALID_REQUEST", message);
    }
}