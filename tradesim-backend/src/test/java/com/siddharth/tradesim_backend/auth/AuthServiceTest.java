package com.siddharth.tradesim_backend.auth;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.exceptions.UserLoginException;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.dto.LoginRequest;
import com.siddharth.tradesim_backend.auth.model.dto.LoginResponse;
import com.siddharth.tradesim_backend.auth.model.dto.RegisterRequest;
import com.siddharth.tradesim_backend.auth.model.dto.RegisterResponse;
import com.siddharth.tradesim_backend.auth.service.AuthService;
import com.siddharth.tradesim_backend.auth.service.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterCompanyRepresentativeSuccessfully() {
        UUID userId = UUID.randomUUID();
        RegisterRequest request = new RegisterRequest(
                "representative1",
                "representative1@example.com",
                "Representative@123"
        );

        when(authRepository.existsByEmail(request.email())).thenReturn(false);
        when(authRepository.existsByUsername(request.username())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(authRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });

        RegisterResponse response = authService.registerCompanyRepresentative(request);

        assertNotNull(response);
        assertEquals(userId, response.id());
        assertEquals(Role.COMPANY_REPRESENTATIVE, response.role());
        assertEquals(AccountStatus.ACTIVE, response.accountStatus());
    }

    @Test
    void shouldLoginUserSuccessfullyWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("sid", "password");

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("sid")
                .email("sid@test.com")
                .password("encoded")
                .role(Role.USER)
                .balance(BigDecimal.valueOf(1000))
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(authRepository.findByUsernameOrEmail("sid")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        LoginResponse response = authService.loginUser(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals("sid", response.username());
        assertEquals(Role.USER, response.role());

        verify(authRepository).save(user);
    }

    @Test
    void shouldThrowExceptionWhenCredentialsAreInvalid() {
        LoginRequest request = new LoginRequest("sid", "wrong");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(UserLoginException.class, () -> authService.loginUser(request));

        verify(authRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void shouldThrowExceptionWhenAccountIsSuspended() {
        LoginRequest request = new LoginRequest("sid", "password");

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("sid")
                .email("sid@test.com")
                .password("encoded")
                .role(Role.USER)
                .balance(BigDecimal.valueOf(1000))
                .accountStatus(AccountStatus.SUSPENDED)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mock(Authentication.class));
        when(authRepository.findByUsernameOrEmail("sid")).thenReturn(Optional.of(user));

        assertThrows(UserLoginException.class, () -> authService.loginUser(request));

        verify(authRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void shouldThrowExceptionWhenAccountIsBanned() {
        LoginRequest request = new LoginRequest("sid", "password");

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("sid")
                .email("sid@test.com")
                .password("encoded")
                .role(Role.USER)
                .balance(BigDecimal.valueOf(1000))
                .accountStatus(AccountStatus.BANNED)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mock(Authentication.class));
        when(authRepository.findByUsernameOrEmail("sid")).thenReturn(Optional.of(user));

        assertThrows(UserLoginException.class, () -> authService.loginUser(request));

        verify(authRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any());
    }
}