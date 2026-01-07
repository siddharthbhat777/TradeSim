package com.siddharth.tradesim_backend.user;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.order.TradeRepository;
import com.siddharth.tradesim_backend.order.enums.Status;
import com.siddharth.tradesim_backend.order.model.Trade;
import com.siddharth.tradesim_backend.user.exceptions.StatusException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
    private TradeRepository tradeRepository;

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
    void shouldBanUserAndCancelAllPendingTrades() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .balance(new BigDecimal("50000"))
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        Trade trade1 = Trade.builder().status(Status.PENDING).build();
        Trade trade2 = Trade.builder().status(Status.PENDING).build();

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tradeRepository.findByUserIdAndStatus(userId, Status.PENDING)).thenReturn(List.of(trade1, trade2));

        userService.changeStatus(userId, AccountStatus.BANNED);

        assertEquals(AccountStatus.BANNED, user.getAccountStatus());
        assertEquals(BigDecimal.ZERO, user.getBalance());
        assertEquals(Status.CANCELLED, trade1.getStatus());
        assertEquals(Status.CANCELLED, trade2.getStatus());

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