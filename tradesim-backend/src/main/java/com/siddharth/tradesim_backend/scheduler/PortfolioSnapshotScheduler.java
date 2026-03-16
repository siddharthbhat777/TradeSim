package com.siddharth.tradesim_backend.scheduler;

import com.siddharth.tradesim_backend.portfolio.service.PortfolioSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortfolioSnapshotScheduler {
    private final PortfolioSnapshotService portfolioSnapshotService;

    @Scheduled(cron = "0 0 0 * * *")
    public void runDailySnapshot() {
        portfolioSnapshotService.createDailySnapshots();
    }
}