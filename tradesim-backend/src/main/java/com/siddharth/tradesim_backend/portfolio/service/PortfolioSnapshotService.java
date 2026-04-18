package com.siddharth.tradesim_backend.portfolio.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.portfolio.PortfolioSnapshotRepository;
import com.siddharth.tradesim_backend.portfolio.model.PortfolioSnapshot;
import com.siddharth.tradesim_backend.portfolio.model.dto.PortfolioResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioSnapshotService {
    private final AuthRepository authRepository;
    private final PortfolioService portfolioService;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final Clock clock;

    @Transactional
    public void createDailySnapshots() {
        List<User> users = authRepository.findAll();
        LocalDate snapshotDate = LocalDate.now(clock);

        for (User user : users) {
            try {
                PortfolioResponse portfolio = portfolioService.fetchPortfolio(user.getId());

                PortfolioSnapshot snapshot = snapshotRepository.findByUserIdAndSnapshotDate(user.getId(), snapshotDate)
                        .orElseGet(() -> PortfolioSnapshot.builder()
                                .userId(user.getId())
                                .snapshotDate(snapshotDate)
                                .build());

                snapshot.setTotalValue(portfolio.totalValue());
                snapshot.setRealizedPnl(portfolio.totalRealizedPnl());
                snapshot.setUnrealizedPnl(portfolio.totalUnrealizedPnl());
                snapshot.setEquity(portfolio.equity());
                snapshotRepository.save(snapshot);
            } catch (RuntimeException exception) {
                log.error("Failed to create portfolio snapshot for user {}", user.getId(), exception);
            }
        }
    }
}