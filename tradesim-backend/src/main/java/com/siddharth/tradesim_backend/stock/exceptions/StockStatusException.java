package com.siddharth.tradesim_backend.stock.exceptions;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;

public class StockStatusException extends BusinessException {
    public StockStatusException(String message) {
        super(message);
    }
}
