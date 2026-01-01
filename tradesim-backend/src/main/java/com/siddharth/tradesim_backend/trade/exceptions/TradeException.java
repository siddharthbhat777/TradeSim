package com.siddharth.tradesim_backend.trade.exceptions;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;

public class TradeException extends BusinessException {
    public TradeException(String message) {
        super(message);
    }
}