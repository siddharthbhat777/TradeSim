package com.siddharth.tradesim_backend.portfolio;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class PortfolioException extends BusinessException {
    private PortfolioException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static PortfolioException notFound(String message) {
        return new PortfolioException(HttpStatus.NOT_FOUND, "PORTFOLIO_NOT_FOUND", message);
    }

    public static PortfolioException conflict(String message) {
        return new PortfolioException(HttpStatus.CONFLICT, "PORTFOLIO_CONFLICT", message);
    }

    public static PortfolioException badRequest(String message) {
        return new PortfolioException(HttpStatus.BAD_REQUEST, "PORTFOLIO_INVALID_REQUEST", message);
    }
}