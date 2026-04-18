package com.siddharth.tradesim_backend.company;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class CompanyException extends BusinessException {
    private CompanyException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static CompanyException notFound(String message) {
        return new CompanyException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", message);
    }

    public static CompanyException conflict(String message) {
        return new CompanyException(HttpStatus.CONFLICT, "COMPANY_CONFLICT", message);
    }

    public static CompanyException badRequest(String message) {
        return new CompanyException(HttpStatus.BAD_REQUEST, "COMPANY_INVALID_REQUEST", message);
    }

    public static CompanyException forbidden(String message) {
        return new CompanyException(HttpStatus.FORBIDDEN, "COMPANY_FORBIDDEN", message);
    }
}