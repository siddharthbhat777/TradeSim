package com.siddharth.tradesim_backend.auth;

import com.siddharth.tradesim_backend.auth.models.dto.LoginRequest;
import com.siddharth.tradesim_backend.auth.models.dto.LoginResponse;
import com.siddharth.tradesim_backend.auth.models.dto.RegisterRequest;
import com.siddharth.tradesim_backend.auth.models.dto.RegisterResponse;
import com.siddharth.tradesim_backend.auth.services.AuthService;
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

    /*@GetMapping("/dev/jwt/generate-key")
    public String generateJwtSecretKey() {
        byte[] keyBytes = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS512)
                .getEncoded();
        return Base64.getEncoder().encodeToString(keyBytes);
    }*/
}