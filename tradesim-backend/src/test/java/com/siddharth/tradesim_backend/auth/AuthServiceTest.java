package com.siddharth.tradesim_backend.auth;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.dto.*;
import com.siddharth.tradesim_backend.auth.repository.AuthRepository;
import com.siddharth.tradesim_backend.auth.repository.RefreshTokenRepository;
import com.siddharth.tradesim_backend.auth.service.AuthService;
import com.siddharth.tradesim_backend.auth.service.JwtService;
import com.siddharth.tradesim_backend.auth.service.OtpService;
import com.siddharth.tradesim_backend.auth.service.RefreshTokenService;
import com.siddharth.tradesim_backend.forex.model.SupportedCurrency;
import com.siddharth.tradesim_backend.forex.repository.SupportedCurrencyRepository;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.order.service.OrderLifecycleService;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.wallet.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private TradingAccountService tradingAccountService;

    @Mock
    private WalletService walletService;

    @Mock
    private SupportedCurrencyRepository supportedCurrencyRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderLifecycleService orderLifecycleService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private OtpService otpService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authRepository,
                passwordEncoder,
                jwtService,
                tradingAccountService,
                walletService,
                refreshTokenService,
                supportedCurrencyRepository,
                orderRepository,
                orderLifecycleService,
                refreshTokenRepository,
                otpService
        );
    }

    @Test
    void shouldRegisterCompanyRepresentativeSuccessfully() {
        UUID userId = UUID.randomUUID();
        RegisterRequest request = new RegisterRequest(
                "Representative One",
                "representative1",
                "representative1@example.com",
                "Representative@123",
                "HDFC Bank",
                "IN",
                null,
                "123456"
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
        assertEquals("Representative One", response.fullName());
        assertEquals("HDFC Bank", response.linkedBankName());
        assertEquals(Role.COMPANY_REPRESENTATIVE, response.role());
        assertEquals(AccountStatus.ACTIVE, response.accountStatus());
        verify(tradingAccountService).createTradingAccountForUser(userId, "INR");
        verify(walletService).createWalletForUser(userId, "INR");
    }

    @Test
    void shouldThrowWhenBaseCurrencyMissingForUnsupportedCountry() {
        RegisterRequest request = new RegisterRequest(
                "Mexico User",
                "mexico_user",
                "mx@example.com",
                "Password@123",
                "Citibanamex",
                "MX",
                null,
                "123456"
        );

        when(authRepository.existsByEmail(request.email())).thenReturn(false);
        when(authRepository.existsByUsername(request.username())).thenReturn(false);
        when(supportedCurrencyRepository.findById("MXN")).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class, () -> authService.registerUser(request));
        assertEquals("Base currency is required because your country's native currency is not supported for wallets", exception.getMessage());
    }

    @Test
    void shouldRegisterWithExplicitBaseCurrencyForUnsupportedCountry() {
        UUID userId = UUID.randomUUID();
        RegisterRequest request = new RegisterRequest(
                "Mexico User",
                "mexico_user",
                "mx@example.com",
                "Password@123",
                "Citibanamex",
                "MX",
                "EUR",
                "123456"
        );

        SupportedCurrency eurCurrency = SupportedCurrency.builder().code("EUR").isActive(true).build();

        when(authRepository.existsByEmail(request.email())).thenReturn(false);
        when(authRepository.existsByUsername(request.username())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(supportedCurrencyRepository.findById("MXN")).thenReturn(Optional.empty());
        when(supportedCurrencyRepository.findById("EUR")).thenReturn(Optional.of(eurCurrency));
        when(authRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });

        RegisterResponse response = authService.registerUser(request);

        assertNotNull(response);
        verify(tradingAccountService).createTradingAccountForUser(userId, "EUR");
        verify(walletService).createWalletForUser(userId, "EUR");
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
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(authRepository.findByUsernameOrEmail("sid")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-accessToken");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-token");

        AuthTokenResult response = authService.loginUser(request);

        assertNotNull(response);
        assertEquals("jwt-accessToken", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("sid", response.username());
        assertEquals(Role.USER, response.role());

        verify(authRepository).save(user);
    }

    @Test
    void shouldThrowExceptionWhenPasswordIsInvalid() {
        LoginRequest request = new LoginRequest("sid", "wrong");

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("sid")
                .email("sid@test.com")
                .password("encoded")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(authRepository.findByUsernameOrEmail("sid")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        AuthException exception = assertThrows(AuthException.class, () -> authService.loginUser(request));

        assertEquals("AUTH_UNAUTHORIZED", exception.getErrorCode());
        assertEquals("Invalid username/email or password", exception.getMessage());

        verify(authRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
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
                .accountStatus(AccountStatus.SUSPENDED)
                .build();

        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(authRepository.findByUsernameOrEmail("sid")).thenReturn(Optional.of(user));

        assertThrows(AuthException.class, () -> authService.loginUser(request));

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
                .accountStatus(AccountStatus.BANNED)
                .build();

        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(authRepository.findByUsernameOrEmail("sid")).thenReturn(Optional.of(user));

        assertThrows(AuthException.class, () -> authService.loginUser(request));

        verify(authRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void shouldThrowExceptionWhenAccountIsDeactivated() {
        LoginRequest request = new LoginRequest("sid", "password");

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("sid")
                .email("sid@test.com")
                .password("encoded")
                .role(Role.USER)
                .accountStatus(AccountStatus.DEACTIVATED)
                .build();

        when(authRepository.findByUsernameOrEmail("sid")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);

        AuthException exception = assertThrows(AuthException.class, () -> authService.loginUser(request));

        assertEquals("AUTH_ACCOUNT_DEACTIVATED", exception.getErrorCode());

        verify(authRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void shouldReactivateDeactivatedAccountSuccessfully() {
        ReactivateRequest request = new ReactivateRequest("sid", "password");

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("sid")
                .email("sid@test.com")
                .password("encoded")
                .role(Role.USER)
                .accountStatus(AccountStatus.DEACTIVATED)
                .build();

        when(authRepository.findByUsernameOrEmail("sid")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-accessToken");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-token");

        AuthTokenResult response = authService.reactivateAccount(request);

        assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());
        assertEquals("jwt-accessToken", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("sid", response.username());

        verify(authRepository).save(user);
    }

    @Test
    void shouldDeactivateAccountSuccessfully() {
        UUID userId = UUID.randomUUID();
        DeactivateRequest request = new DeactivateRequest("password123");

        User user = User.builder()
                .id(userId)
                .password("encoded")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        Order openOrder = Order.builder().status(OrderStatus.OPEN).build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(orderRepository.findByUserIdAndStatusIn(eq(userId), eq(List.of(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED)))).thenReturn(List.of(openOrder));

        authService.deactivateAccount(userId, request);

        assertEquals(AccountStatus.DEACTIVATED, user.getAccountStatus());
        verify(orderLifecycleService).cancelOrder(openOrder);
        verify(authRepository).save(user);
        verify(refreshTokenRepository).revokeActiveTokensForUser(eq(userId), any());
    }

    @Test
    void shouldThrowWhenInvalidPasswordForDeactivate() {
        UUID userId = UUID.randomUUID();
        DeactivateRequest request = new DeactivateRequest("wrongpass");

        User user = User.builder()
                .id(userId)
                .password("encoded")
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "encoded")).thenReturn(false);

        assertThrows(AuthException.class, () -> authService.deactivateAccount(userId, request));
    }
}