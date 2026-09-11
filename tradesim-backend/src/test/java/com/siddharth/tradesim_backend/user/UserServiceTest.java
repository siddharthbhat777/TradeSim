package com.siddharth.tradesim_backend.user;

import com.siddharth.tradesim_backend.auth.AuthException;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.enums.ThemePreference;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.repository.AuthRepository;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentStatus;
import com.siddharth.tradesim_backend.company.repository.CompanyRepresentativeAssignmentRepository;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.order.service.OrderLifecycleService;
import com.siddharth.tradesim_backend.user.dto.BankBalanceRequest;
import com.siddharth.tradesim_backend.user.dto.BankBalanceResponse;
import com.siddharth.tradesim_backend.user.dto.EditProfileRequest;
import com.siddharth.tradesim_backend.user.dto.UserProfileResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderLifecycleService orderLifecycleService;

    @Mock
    private CompanyRepresentativeAssignmentRepository companyRepresentativeAssignmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldFetchUserProfileSuccessfully() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .fullName("Siddharth Bhat")
                .username("sid")
                .email("sid@test.com")
                .linkedBankName("HDFC Bank")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .themePreference(ThemePreference.SYSTEM)
                .countryCode("IN")
                .bankBalance(BigDecimal.valueOf(1000000))
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.fetchUserProfile(userId);

        assertEquals(userId, response.id());
        assertEquals("Siddharth Bhat", response.fullName());
        assertEquals("HDFC Bank", response.linkedBankName());
        assertEquals(ThemePreference.SYSTEM, response.themePreference());
        assertEquals("IN", response.countryCode());
    }

    @Test
    void shouldEditProfileSuccessfully() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .fullName("Old Name")
                .username("sid")
                .email("sid@test.com")
                .linkedBankName("Old Bank")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .themePreference(ThemePreference.SYSTEM)
                .countryCode("IN")
                .bankBalance(BigDecimal.valueOf(1000000))
                .build();

        EditProfileRequest request = new EditProfileRequest("New Name", "New Bank");

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userService.editProfile(userId, request);

        assertEquals("New Name", response.fullName());
        assertEquals("New Bank", response.linkedBankName());
        verify(authRepository).save(user);
    }

    @Test
    void shouldFetchBankBalanceSuccessfully() {
        UUID userId = UUID.randomUUID();
        BankBalanceRequest request = new BankBalanceRequest("password123");

        User user = User.builder()
                .id(userId)
                .password("encoded")
                .bankBalance(BigDecimal.valueOf(50000))
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);

        BankBalanceResponse response = userService.fetchBankBalance(userId, request);

        assertEquals(BigDecimal.valueOf(50000), response.bankBalance());
    }

    @Test
    void shouldThrowWhenInvalidPasswordForBankBalance() {
        UUID userId = UUID.randomUUID();
        BankBalanceRequest request = new BankBalanceRequest("wrongpass");

        User user = User.builder()
                .id(userId)
                .password("encoded")
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "encoded")).thenReturn(false);

        assertThrows(AuthException.class, () -> userService.fetchBankBalance(userId, request));
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        UUID userId = UUID.randomUUID();

        when(authRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserException.class, () -> userService.changeStatus(userId, AccountStatus.SUSPENDED));
    }

    @Test
    void shouldNotAllowStatusChangeForBannedUser() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .accountStatus(AccountStatus.BANNED)
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(UserException.class, () -> userService.changeStatus(userId, AccountStatus.ACTIVE));
    }

    @Test
    void shouldNotAllowAdminToDeactivateUser() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(UserException.class, () -> userService.changeStatus(userId, AccountStatus.DEACTIVATED));
    }

    @Test
    void shouldBanUserAndCancelAllOpenAndPartiallyFilledOrders() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        Order openOrder = Order.builder()
                .status(OrderStatus.OPEN)
                .build();

        Order partialOrder = Order.builder()
                .status(OrderStatus.PARTIALLY_FILLED)
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdAndStatusIn(eq(userId), eq(List.of(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED)))).thenReturn(List.of(openOrder, partialOrder));
        when(authRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.changeStatus(userId, AccountStatus.BANNED);

        assertEquals(AccountStatus.BANNED, user.getAccountStatus());
        verify(orderLifecycleService).cancelOrder(openOrder);
        verify(orderLifecycleService).cancelOrder(partialOrder);
        verify(authRepository).save(user);
    }

    @Test
    void shouldChangeUserStatusNormally() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.changeStatus(userId, AccountStatus.SUSPENDED);

        assertEquals(AccountStatus.SUSPENDED, user.getAccountStatus());
        verify(authRepository).save(user);
    }

    @Test
    void shouldChangeRoleFromUserToCompanyRepresentative() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .username("normal1")
                .email("normal1@example.com")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.changeRole(userId, Role.COMPANY_REPRESENTATIVE);

        assertEquals(Role.COMPANY_REPRESENTATIVE, user.getRole());
        verify(authRepository).save(user);
    }

    @Test
    void shouldRejectAdminRoleAssignment() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(UserException.class, () -> userService.changeRole(userId, Role.ADMIN));
    }

    @Test
    void shouldRejectChangingRoleOfAdminUser() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(UserException.class, () -> userService.changeRole(userId, Role.USER));
    }

    @Test
    void shouldRejectDemotionWhenActiveCompanyAssignmentsExist() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .role(Role.COMPANY_REPRESENTATIVE)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(companyRepresentativeAssignmentRepository.existsByUserIdAndStatus(userId, CompanyRepresentativeAssignmentStatus.ACTIVE)).thenReturn(true);

        assertThrows(UserException.class, () -> userService.changeRole(userId, Role.USER));
    }
}