package com.siddharth.tradesim_backend.portfolio;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.portfolio.model.PortfolioSnapshot;
import com.siddharth.tradesim_backend.portfolio.model.dto.PortfolioResponse;
import com.siddharth.tradesim_backend.portfolio.service.PortfolioService;
import com.siddharth.tradesim_backend.portfolio.service.PortfolioSnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioSnapshotServiceTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private PortfolioSnapshotRepository portfolioSnapshotRepository;

    private PortfolioSnapshotService portfolioSnapshotService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-18T08:00:00Z"), ZoneOffset.UTC);
        portfolioSnapshotService = new PortfolioSnapshotService(
                authRepository,
                portfolioService,
                portfolioSnapshotRepository,
                clock
        );
    }

    @Test
    void shouldUpdateExistingSnapshotForSameDay() {
        UUID userId = UUID.randomUUID();
        LocalDate snapshotDate = LocalDate.of(2026, 4, 18);

        User user = User.builder().id(userId).build();
        PortfolioSnapshot existingSnapshot = PortfolioSnapshot.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .snapshotDate(snapshotDate)
                .totalValue(BigDecimal.ONE)
                .realizedPnl(BigDecimal.ONE)
                .unrealizedPnl(BigDecimal.ONE)
                .equity(BigDecimal.ONE)
                .build();

        when(authRepository.findAll()).thenReturn(List.of(user));
        when(portfolioService.fetchPortfolio(userId)).thenReturn(portfolioResponse(BigDecimal.valueOf(2500)));
        when(portfolioSnapshotRepository.findByUserIdAndSnapshotDate(userId, snapshotDate)).thenReturn(Optional.of(existingSnapshot));

        portfolioSnapshotService.createDailySnapshots();

        ArgumentCaptor<PortfolioSnapshot> snapshotCaptor = ArgumentCaptor.forClass(PortfolioSnapshot.class);
        verify(portfolioSnapshotRepository).save(snapshotCaptor.capture());

        PortfolioSnapshot savedSnapshot = snapshotCaptor.getValue();
        assertThat(savedSnapshot.getId()).isEqualTo(existingSnapshot.getId());
        assertThat(savedSnapshot.getSnapshotDate()).isEqualTo(snapshotDate);
        assertThat(savedSnapshot.getTotalValue()).isEqualByComparingTo("2500");
        assertThat(savedSnapshot.getEquity()).isEqualByComparingTo("2600");
    }

    @Test
    void shouldContinueCreatingSnapshotsWhenOneUserFails() {
        UUID failingUserId = UUID.randomUUID();
        UUID healthyUserId = UUID.randomUUID();
        LocalDate snapshotDate = LocalDate.of(2026, 4, 18);

        User failingUser = User.builder().id(failingUserId).build();
        User healthyUser = User.builder().id(healthyUserId).build();

        when(authRepository.findAll()).thenReturn(List.of(failingUser, healthyUser));
        when(portfolioService.fetchPortfolio(failingUserId)).thenThrow(new IllegalStateException("Portfolio unavailable"));
        when(portfolioService.fetchPortfolio(healthyUserId)).thenReturn(portfolioResponse(BigDecimal.valueOf(500)));
        when(portfolioSnapshotRepository.findByUserIdAndSnapshotDate(healthyUserId, snapshotDate)).thenReturn(Optional.empty());

        portfolioSnapshotService.createDailySnapshots();

        verify(portfolioSnapshotRepository, never()).findByUserIdAndSnapshotDate(failingUserId, snapshotDate);
        verify(portfolioSnapshotRepository).save(org.mockito.ArgumentMatchers.argThat(snapshot ->
                snapshot.getUserId().equals(healthyUserId)
                        && snapshot.getSnapshotDate().equals(snapshotDate)
                        && snapshot.getTotalValue().compareTo(BigDecimal.valueOf(500)) == 0
        ));
    }

    private PortfolioResponse portfolioResponse(BigDecimal totalValue) {
        return new PortfolioResponse(
                List.of(),
                totalValue,
                BigDecimal.valueOf(400),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(150),
                totalValue.add(BigDecimal.valueOf(100))
        );
    }
}