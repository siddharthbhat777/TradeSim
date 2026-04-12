package com.siddharth.tradesim_backend.auth.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.AuthException;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.dto.LoginRequest;
import com.siddharth.tradesim_backend.auth.model.dto.LoginResponse;
import com.siddharth.tradesim_backend.auth.model.dto.RegisterRequest;
import com.siddharth.tradesim_backend.auth.model.dto.RegisterResponse;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    private final AuthenticationManager authenticationManager;
    private final TradingAccountService tradingAccountService;

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
    public LoginResponse loginUser(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.usernameOrEmail(),
                            request.password()
                    )
            );

            User user = authRepository.findByUsernameOrEmail(request.usernameOrEmail()).orElseThrow(() -> AuthException.unauthorized("User not found"));

            if (user.getAccountStatus() == AccountStatus.SUSPENDED || user.getAccountStatus() == AccountStatus.BANNED) {
                throw AuthException.forbidden("Cannot login, your account is " + user.getAccountStatus());
            }

            user.setLastLogin(Instant.now());
            authRepository.save(user);

            return new LoginResponse(jwtService.generateToken(user), user.getUsername(), user.getRole());
        } catch (BadCredentialsException e) {
            throw AuthException.unauthorized("Invalid username or password");
        }
    }
}