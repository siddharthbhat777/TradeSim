package com.siddharth.tradesim_backend.risk;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class RiskException extends BusinessException {
    private RiskException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static RiskException notFound(String message) {
        return new RiskException(HttpStatus.NOT_FOUND, "RISK_NOT_FOUND", message);
    }

    public static RiskException conflict(String message) {
        return new RiskException(HttpStatus.CONFLICT, "RISK_CONFLICT", message);
    }
}