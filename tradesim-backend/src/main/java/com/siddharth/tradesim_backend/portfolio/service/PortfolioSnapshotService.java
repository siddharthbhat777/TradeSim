package com.siddharth.tradesim_backend.portfolio.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.portfolio.PortfolioSnapshotRepository;
import com.siddharth.tradesim_backend.portfolio.model.PortfolioSnapshot;
import com.siddharth.tradesim_backend.portfolio.model.dto.PortfolioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioSnapshotService {
    private final AuthRepository authRepository;
    private final PortfolioService portfolioService;
    private final PortfolioSnapshotRepository snapshotRepository;

    @Transactional
    public void createDailySnapshots() {
        List<User> users = authRepository.findAll();

        for (User user : users) {
            PortfolioResponse portfolio = portfolioService.fetchPortfolio(user.getId());

            PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                    .userId(user.getId())
                    .snapshotDate(LocalDate.now())
                    .totalValue(portfolio.totalValue())
                    .realizedPnl(portfolio.totalRealizedPnl())
                    .unrealizedPnl(portfolio.totalUnrealizedPnl())
                    .equity(portfolio.equity())
                    .build();

            snapshotRepository.save(snapshot);
        }
    }
}