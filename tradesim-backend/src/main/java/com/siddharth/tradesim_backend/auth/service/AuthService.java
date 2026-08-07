package com.siddharth.tradesim_backend.auth.service;

import com.siddharth.tradesim_backend.auth.model.dto.*;
import com.siddharth.tradesim_backend.auth.repository.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.AuthException;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Currency;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid username/email or password";

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TradingAccountService tradingAccountService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public RegisterResponse registerUser(RegisterRequest request) {
        return registerUserWithRole(request, Role.USER);
    }

    @Transactional
    public RegisterResponse registerCompanyRepresentative(RegisterRequest request) {
        return registerUserWithRole(request, Role.COMPANY_REPRESENTATIVE);
    }

    @Transactional
    public AuthTokenResult loginUser(LoginRequest request) {
        User user = authenticateCredentials(request.usernameOrEmail(), request.password());

        assertCanLogin(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthTokenResult reactivateAccount(ReactivateRequest request) {
        User user = authenticateCredentials(request.usernameOrEmail(), request.password());

        assertCanReactivate(user);

        user.setAccountStatus(AccountStatus.ACTIVE);

        return issueTokens(user);
    }

    @Transactional
    public AuthTokenResult refreshAccessToken(String rawRefreshToken) {
        RefreshTokenRotation rotation = refreshTokenService.rotate(rawRefreshToken);
        User user = rotation.user();

        return new AuthTokenResult(
                jwtService.generateToken(user),
                rotation.refreshToken(),
                user.getUsername(),
                user.getRole()
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private RegisterResponse registerUserWithRole(RegisterRequest request, Role role) {
        if (authRepository.existsByEmail(request.email())) {
            throw AuthException.conflict("Email already exists");
        }
        if (authRepository.existsByUsername(request.username())) {
            throw AuthException.conflict("Username already exists");
        }

        try {
            User user = User.builder()
                    .username(request.username())
                    .email(request.email())
                    .password(passwordEncoder.encode(request.password()))
                    .role(role)
                    .accountStatus(AccountStatus.ACTIVE)
                    .country(request.countryCode())
                    .build();

            User saved = authRepository.save(user);

            String baseCurrency = resolveCurrencyFromCountryCode(request.countryCode());
            tradingAccountService.createTradingAccountForUser(saved.getId(), baseCurrency);

            return new RegisterResponse(
                    saved.getId(),
                    saved.getUsername(),
                    saved.getEmail(),
                    saved.getRole(),
                    saved.getAccountStatus()
            );
        } catch (DataIntegrityViolationException e) {
            throw AuthException.badRequest("Invalid user data");
        }
    }

    private String resolveCurrencyFromCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return "INR";
        }
        try {
            Locale locale = Locale.of("", countryCode.trim().toUpperCase());
            return Currency.getInstance(locale).getCurrencyCode();
        } catch (IllegalArgumentException e) {
            return "INR";
        }
    }

    private void assertCanLogin(User user) {
        switch (user.getAccountStatus()) {
            case ACTIVE -> {
            }
            case SUSPENDED -> throw AuthException.accountSuspended("Your account is suspended.");
            case BANNED -> throw AuthException.accountBanned("Your account is banned.");
            case DEACTIVATED ->
                    throw AuthException.accountDeactivated("Your account is deactivated. Reactivation required.");
        }
    }

    private User authenticateCredentials(String usernameOrEmail, String password) {
        User user = authRepository.findByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> AuthException.unauthorized(INVALID_CREDENTIALS_MESSAGE));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw AuthException.unauthorized(INVALID_CREDENTIALS_MESSAGE);
        }

        return user;
    }

    private AuthTokenResult issueTokens(User user) {
        user.setLastLogin(Instant.now());
        authRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthTokenResult(accessToken, refreshToken, user.getUsername(), user.getRole());
    }

    private void assertCanReactivate(User user) {
        switch (user.getAccountStatus()) {
            case DEACTIVATED -> {
            }
            case ACTIVE -> throw AuthException.conflict("Account is already active.");
            case SUSPENDED -> throw AuthException.accountSuspended("Your account is suspended.");
            case BANNED -> throw AuthException.accountBanned("Your account is banned.");
        }
    }
}