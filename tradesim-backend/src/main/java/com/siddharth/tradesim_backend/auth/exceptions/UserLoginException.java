package com.siddharth.tradesim_backend.auth.exceptions;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;

public class UserLoginException extends BusinessException {
    public UserLoginException(String message) {
        super(message);
    }
}