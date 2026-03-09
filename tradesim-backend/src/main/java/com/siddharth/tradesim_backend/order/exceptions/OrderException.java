package com.siddharth.tradesim_backend.order.exceptions;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;

public class OrderException extends BusinessException {
    public OrderException(String message) {
        super(message);
    }
}