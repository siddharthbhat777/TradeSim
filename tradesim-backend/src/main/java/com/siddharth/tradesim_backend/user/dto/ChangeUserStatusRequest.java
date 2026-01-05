package com.siddharth.tradesim_backend.user.dto;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;

public record ChangeUserStatusRequest(
        AccountStatus status
) {}