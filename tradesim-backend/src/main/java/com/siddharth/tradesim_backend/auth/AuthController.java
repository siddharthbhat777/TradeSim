package com.siddharth.tradesim_backend.auth;

import com.siddharth.tradesim_backend.auth.model.dto.*;
import com.siddharth.tradesim_backend.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${auth.refresh-token.cookie-name}")
    private String refreshTokenCookieName;

    @Value("${auth.refresh-token.cookie-path}")
    private String refreshTokenCookiePath;

    @Value("${auth.refresh-token.cookie-secure}")
    private boolean refreshTokenCookieSecure;

    @Value("${auth.refresh-token.cookie-same-site}")
    private String refreshTokenCookieSameSite;

    @PostMapping("register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(request));
    }

    @PostMapping("login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return buildAuthResponse(authService.loginUser(request));
    }

    @PostMapping("refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
        return buildAuthResponse(authService.refreshAccessToken(readRefreshTokenCookie(request)));
    }

    @PostMapping("logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(readRefreshTokenCookie(request));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshTokenCookie().toString())
                .build();
    }

    @PostMapping("reactivate")
    public ResponseEntity<LoginResponse> reactivate(@Valid @RequestBody ReactivateRequest request) {
        return buildAuthResponse(authService.reactivateAccount(request));
    }

    private ResponseEntity<LoginResponse> buildAuthResponse(AuthTokenResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie(result.refreshToken()).toString())
                .body(new LoginResponse(result.accessToken(), result.username(), result.role()));
    }

    private ResponseCookie buildRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(refreshTokenCookieName, refreshToken)
                .httpOnly(true)
                .secure(refreshTokenCookieSecure)
                .sameSite(refreshTokenCookieSameSite)
                .path(refreshTokenCookiePath)
                .maxAge(Duration.ofMillis(refreshExpiration))
                .build();
    }

    private ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from(refreshTokenCookieName, "")
                .httpOnly(true)
                .secure(refreshTokenCookieSecure)
                .sameSite(refreshTokenCookieSameSite)
                .path(refreshTokenCookiePath)
                .maxAge(Duration.ZERO)
                .build();
    }

    private String readRefreshTokenCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> refreshTokenCookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}