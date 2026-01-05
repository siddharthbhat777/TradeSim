package com.siddharth.tradesim_backend.user;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.trade.TradeRepository;
import com.siddharth.tradesim_backend.trade.enums.Status;
import com.siddharth.tradesim_backend.trade.model.Trade;
import com.siddharth.tradesim_backend.user.dto.ChangeUserStatusResponse;
import com.siddharth.tradesim_backend.user.exceptions.StatusException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final AuthRepository authRepository;
    private final TradeRepository tradeRepository;

    @Transactional
    public ChangeUserStatusResponse changeStatus(UUID userId, AccountStatus status) {
        User user = authRepository.findById(userId).orElseThrow(() -> new BusinessException("User not found"));
        if (user.getAccountStatus().equals(AccountStatus.BANNED)) throw new StatusException("Cannot change status of banned user");
        if (status.equals(AccountStatus.DEACTIVATED)) throw new StatusException("Account can only be deactivated by account owner");
        if (status.equals(AccountStatus.BANNED)) {
            user.setBalance(BigDecimal.ZERO);
            List<Trade> trades = tradeRepository.findByUserIdAndStatus(userId, Status.PENDING);
            for (Trade trade : trades) {
                trade.setStatus(Status.CANCELLED);
            }
        }
        user.setAccountStatus(status);
        authRepository.save(user);
        return new ChangeUserStatusResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getAccountStatus()
        );
    }
}