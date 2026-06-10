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

@Service
@RequiredArgsConstructor
public class AuthService {
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
                    .build();

            User saved = authRepository.save(user);
            tradingAccountService.createTradingAccountForUser(saved.getId());

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

    @Transactional
    public AuthTokenResult loginUser(LoginRequest request) {
        User user = authRepository.findByUsernameOrEmail(request.usernameOrEmail()).orElseThrow(() -> AuthException.unauthorized("Invalid username/email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw AuthException.unauthorized("Invalid username/email or password");
        }

        assertCanLogin(user);

        user.setLastLogin(Instant.now());
        authRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthTokenResult(accessToken, refreshToken, user.getUsername(), user.getRole());
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
}