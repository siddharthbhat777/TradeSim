package com.siddharth.tradesim_backend.auth;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.exceptions.UserRegistrationException;
import com.siddharth.tradesim_backend.auth.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(User user) {
        if (authRepository.existsByEmail(user.getEmail())) {
            throw new UserRegistrationException("Email already exists");
        }
        if (authRepository.existsByUsername(user.getUsername())) {
            throw new UserRegistrationException("Username already exists");
        }

        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setRole(Role.USER);
            user.setBalance(BigDecimal.ZERO);
            user.setAccountStatus(AccountStatus.ACTIVE);
            return authRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new UserRegistrationException("Invalid user data");
        } catch (Exception e) {
            throw new UserRegistrationException("Unable to register user");
        }
    }
}
