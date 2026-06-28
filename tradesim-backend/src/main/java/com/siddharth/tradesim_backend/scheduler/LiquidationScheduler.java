package com.siddharth.tradesim_backend.scheduler;

import com.siddharth.tradesim_backend.auth.repository.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.risk.service.RiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LiquidationScheduler {
    private final AuthRepository authRepository;
    private final RiskService riskService;

    @Scheduled(fixedRate = 5000)
    public void checkAllUsersForLiquidation() {
        List<User> users = authRepository.findByAccountStatus(AccountStatus.ACTIVE);

        for (User user : users) {
            try {
                riskService.checkLiquidation(user.getId());
            } catch (Exception e) {
                log.error("Liquidation check failed for user: {}", user.getId(), e);
            }
        }
    }
}