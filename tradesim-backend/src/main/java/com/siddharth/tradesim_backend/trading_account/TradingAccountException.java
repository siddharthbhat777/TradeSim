package com.siddharth.tradesim_backend.trading_account;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class TradingAccountException extends BusinessException {
    private TradingAccountException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static TradingAccountException notFound(String message) {
        return new TradingAccountException(HttpStatus.NOT_FOUND, "TRADING_ACCOUNT_NOT_FOUND", message);
    }

    public static TradingAccountException conflict(String message) {
        return new TradingAccountException(HttpStatus.CONFLICT, "TRADING_ACCOUNT_CONFLICT", message);
    }

    public static TradingAccountException badRequest(String message) {
        return new TradingAccountException(HttpStatus.BAD_REQUEST, "TRADING_ACCOUNT_INVALID_REQUEST", message);
    }
}