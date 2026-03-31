package com.siddharth.tradesim_backend.user.exceptions;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;

public class RoleException extends BusinessException {
    public RoleException(String message) {
        super(message);
    }
}