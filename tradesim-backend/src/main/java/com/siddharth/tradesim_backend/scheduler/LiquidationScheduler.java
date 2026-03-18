package com.siddharth.tradesim_backend.scheduler;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.risk.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LiquidationScheduler {
    private final AuthRepository authRepository;
    private final RiskService riskService;

    @Scheduled(fixedRate = 5000)
    public void checkAllUsersForLiquidation() {
        List<User> users = authRepository.findAll();

        for (User user : users) {
            try {
                riskService.checkLiquidation(user);
            } catch (Exception e) {
                System.out.println("Liquidation check failed for user: " + user.getId());
            }
        }
    }
}