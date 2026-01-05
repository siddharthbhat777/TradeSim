package com.siddharth.tradesim_backend.user.exceptions;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;

public class StatusException extends BusinessException {
    public StatusException(String message) {
        super(message);
    }
}