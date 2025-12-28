package com.siddharth.tradesim_backend.stock.exceptions;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;

public class CreateStockException extends BusinessException {
    public CreateStockException(String message) {
        super(message);
    }
}