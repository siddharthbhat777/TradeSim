package com.siddharth.tradesim_backend.market_index;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class MarketIndexException extends BusinessException {
    protected MarketIndexException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static MarketIndexException notFound(String message) {
        return new MarketIndexException(HttpStatus.NOT_FOUND, "MARKET_INDEX_NOT_FOUND", message);
    }

    public static MarketIndexException conflict(String message) {
        return new MarketIndexException(HttpStatus.CONFLICT, "MARKET_INDEX_CONFLICT", message);
    }

    public static MarketIndexException badRequest(String message) {
        return new MarketIndexException(HttpStatus.BAD_REQUEST, "MARKET_INDEX_INVALID_REQUEST", message);
    }
}