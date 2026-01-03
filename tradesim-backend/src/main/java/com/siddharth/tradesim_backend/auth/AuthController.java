package com.siddharth.tradesim_backend.auth;

import com.siddharth.tradesim_backend.auth.model.dto.LoginRequest;
import com.siddharth.tradesim_backend.auth.model.dto.LoginResponse;
import com.siddharth.tradesim_backend.auth.model.dto.RegisterRequest;
import com.siddharth.tradesim_backend.auth.model.dto.RegisterResponse;
import com.siddharth.tradesim_backend.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(request));
    }

    @PostMapping("login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginUser(request));
    }
}