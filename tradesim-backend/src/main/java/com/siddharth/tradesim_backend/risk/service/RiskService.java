package com.siddharth.tradesim_backend.risk.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.risk.dto.RiskResponse;
import com.siddharth.tradesim_backend.risk.enums.RiskLevel;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RiskService {
    private final PositionRepository positionRepository;
    private final StockRepository stockRepository;
    private final AuthRepository authRepository;
    private final LiquidationService liquidationService;

    public void validateBuyOrder(User user, BigDecimal orderValue) {
        BigDecimal requiredMargin = orderValue.divide(BigDecimal.valueOf(user.getLeverage()), 4, RoundingMode.HALF_UP);

        if (user.getAvailableBalance().compareTo(requiredMargin) < 0) {
            throw new BusinessException("Insufficient margin");
        }
    }

    public void checkLiquidation(User user) {
        RiskResponse risk = calculateRisk(user);

        if (risk.isUnderLiquidation()) {
            liquidationService.liquidateUser(user);
        }
    }

    public RiskResponse getUserRisk(UUID userId) {
        User user = authRepository.findById(userId).orElseThrow(() -> new BusinessException("User not found"));
        return calculateRisk(user);
    }

    private RiskResponse calculateRisk(User user) {
        List<Position> positions = positionRepository.findByUserId(user.getId());

        BigDecimal totalPositionValue = BigDecimal.ZERO;
        BigDecimal totalUnrealizedPnl = BigDecimal.ZERO;

        for (Position position : positions) {
            Stock stock = stockRepository.findById(position.getStockId()).orElseThrow(() -> new BusinessException("Stock not found"));

            BigDecimal currentPrice = stock.getLastTradedPrice();
            BigDecimal positionValue = currentPrice.multiply(BigDecimal.valueOf(position.getQuantity()));
            BigDecimal unrealizedPnl = currentPrice.subtract(position.getAverageBuyPrice()).multiply(BigDecimal.valueOf(position.getQuantity()));

            totalPositionValue = totalPositionValue.add(positionValue);
            totalUnrealizedPnl = totalUnrealizedPnl.add(unrealizedPnl);
        }

        BigDecimal equity = user.calculateEquity(totalPositionValue);
        BigDecimal marginUsed = BigDecimal.ZERO;
        if (totalPositionValue.compareTo(BigDecimal.ZERO) > 0) {
            marginUsed = totalPositionValue.divide(BigDecimal.valueOf(user.getLeverage()), 4, RoundingMode.HALF_UP);
        }

        BigDecimal maintenanceMargin = marginUsed.multiply(user.getMaintenanceMarginPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal marginRatio = BigDecimal.ZERO;
        if (maintenanceMargin.compareTo(BigDecimal.ZERO) > 0) {
            marginRatio = equity.divide(maintenanceMargin, 4, RoundingMode.HALF_UP);
        }

        RiskLevel riskLevel;
        if (maintenanceMargin.compareTo(BigDecimal.ZERO) == 0) {
            riskLevel = RiskLevel.SAFE;
        } else if (marginRatio.compareTo(BigDecimal.ONE) < 0) {
            riskLevel = RiskLevel.LIQUIDATION;
        } else if (marginRatio.compareTo(BigDecimal.valueOf(2)) < 0) {
            riskLevel = RiskLevel.WARNING;
        } else {
            riskLevel = RiskLevel.SAFE;
        }

        boolean isUnderLiquidation = equity.compareTo(maintenanceMargin) < 0;

        return new RiskResponse(
                equity,
                marginUsed,
                maintenanceMargin,
                totalUnrealizedPnl,
                marginRatio,
                riskLevel,
                isUnderLiquidation
        );
    }
}