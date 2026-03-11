package com.siddharth.tradesim_backend.auth.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.exceptions.UserLoginException;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.dto.LoginRequest;
import com.siddharth.tradesim_backend.auth.model.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthRepository authRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

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