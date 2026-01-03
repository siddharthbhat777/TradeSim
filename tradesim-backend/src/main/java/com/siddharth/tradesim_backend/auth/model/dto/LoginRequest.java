package com.siddharth.tradesim_backend.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username or E-Mail is required")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    private String password;
}