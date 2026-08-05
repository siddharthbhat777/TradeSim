package com.siddharth.tradesim_backend.scheduler;

import com.siddharth.tradesim_backend.forex.service.ForexIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ForexScheduler {
    private final ForexIntegrationService forexIntegrationService;

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 0 * * * *")
    public void scheduleForexRateSync() {
        forexIntegrationService.fetchAndStoreExchangeRates();
    }
}