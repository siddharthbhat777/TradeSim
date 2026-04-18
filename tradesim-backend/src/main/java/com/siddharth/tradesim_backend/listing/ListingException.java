package com.siddharth.tradesim_backend.listing;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class ListingException extends BusinessException {
    private ListingException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static ListingException notFound(String message) {
        return new ListingException(HttpStatus.NOT_FOUND, "LISTING_NOT_FOUND", message);
    }

    public static ListingException conflict(String message) {
        return new ListingException(HttpStatus.CONFLICT, "LISTING_CONFLICT", message);
    }

    public static ListingException badRequest(String message) {
        return new ListingException(HttpStatus.BAD_REQUEST, "LISTING_INVALID_REQUEST", message);
    }
}