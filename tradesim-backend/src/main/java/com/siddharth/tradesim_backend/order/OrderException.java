package com.siddharth.tradesim_backend.order;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

public class OrderException extends BusinessException {
    public OrderException(String message) {
        super(HttpStatus.BAD_REQUEST, "ORDER_INVALID_REQUEST", message);
    }

    private OrderException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static OrderException notFound(String message) {
        return new OrderException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", message);
    }

    public static OrderException conflict(String message) {
        return new OrderException(HttpStatus.CONFLICT, "ORDER_CONFLICT", message);
    }

    public static OrderException badRequest(String message) {
        return new OrderException(HttpStatus.BAD_REQUEST, "ORDER_INVALID_REQUEST", message);
    }

    public static OrderException forbidden(String message) {
        return new OrderException(HttpStatus.FORBIDDEN, "ORDER_FORBIDDEN", message);
    }
}