package com.siddharth.tradesim_backend.auth.exceptions;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;

public class UserRegistrationException extends BusinessException {
    public UserRegistrationException(String message) {
        super(message);
    }
}