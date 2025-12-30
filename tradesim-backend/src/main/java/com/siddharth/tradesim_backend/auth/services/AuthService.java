package com.siddharth.tradesim_backend.auth.services;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.exceptions.UserLoginException;
import com.siddharth.tradesim_backend.auth.exceptions.UserRegistrationException;
import com.siddharth.tradesim_backend.auth.models.User;
import com.siddharth.tradesim_backend.auth.models.dto.LoginRequest;
import com.siddharth.tradesim_backend.auth.models.dto.LoginResponse;
import com.siddharth.tradesim_backend.auth.models.dto.RegisterRequest;
import com.siddharth.tradesim_backend.auth.models.dto.RegisterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public RegisterResponse registerUser(RegisterRequest request) {
        if (authRepository.existsByEmail(request.getEmail())) {
            throw new UserRegistrationException("Email already exists");
        }
        if (authRepository.existsByUsername(request.getUsername())) {
            throw new UserRegistrationException("Username already exists");
        }

        try {
            User user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(Role.USER)
                    .balance(BigDecimal.valueOf(10000000))
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            User saved = authRepository.save(user);

            return new RegisterResponse(
                    saved.getId(),
                    saved.getUsername(),
                    saved.getEmail(),
                    saved.getRole(),
                    saved.getAccountStatus()
            );
        } catch (DataIntegrityViolationException e) {
            throw new UserRegistrationException("Invalid user data");
        } catch (Exception e) {
            throw new UserRegistrationException("Unable to register user");
        }
    }

    public LoginResponse loginUser(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );

            User user = authRepository
                    .findByUsernameOrEmail(request.getUsernameOrEmail())
                    .orElseThrow(() -> new UserLoginException("User not found"));

            if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                throw new UserLoginException("Account is not active");
            }

            user.setLastLogin(Instant.now());
            authRepository.save(user);

            return new LoginResponse(jwtService.generateToken(user), user.getUsername(), user.getRole());
        } catch (BadCredentialsException e) {
            throw new UserLoginException("Invalid username or password");
        } catch (Exception e) {
            throw new UserLoginException("Unable to login");
        }
    }
}