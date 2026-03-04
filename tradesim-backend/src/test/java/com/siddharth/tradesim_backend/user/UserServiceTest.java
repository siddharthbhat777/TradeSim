package com.siddharth.tradesim_backend.user;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.order.service.OrderLifecycleService;
import com.siddharth.tradesim_backend.user.exceptions.StatusException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AuthRepository authRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderLifecycleService orderLifecycleService;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        UUID userId = UUID.randomUUID();

        when(authRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> userService.changeStatus(userId, AccountStatus.SUSPENDED));
    }

    @Test
    void shouldNotAllowStatusChangeForBannedUser() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .accountStatus(AccountStatus.BANNED)
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(StatusException.class, () -> userService.changeStatus(userId, AccountStatus.ACTIVE));
    }

    @Test
    void shouldNotAllowAdminToDeactivateUser() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(StatusException.class, () -> userService.changeStatus(userId, AccountStatus.DEACTIVATED));
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

        userService.changeStatus(userId, AccountStatus.SUSPENDED);

        assertEquals(AccountStatus.SUSPENDED, user.getAccountStatus());
        verify(authRepository).save(user);
    }
}