package com.siddharth.tradesim_backend.risk;

import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskService {
    private final PositionRepository positionRepository;
    private final StockRepository stockRepository;
    private final LiquidationService liquidationService;

    public void validateBuyOrder(User user, BigDecimal orderValue) {
        BigDecimal requiredMargin = orderValue.divide(BigDecimal.valueOf(user.getLeverage()), 4, RoundingMode.HALF_UP);

        if (user.getAvailableBalance().compareTo(requiredMargin) < 0) {
            throw new BusinessException("Insufficient margin");
        }
    }

    public void checkLiquidation(User user) {
        List<Position> positions = positionRepository.findByUserId(user.getId());

        if (positions.isEmpty()) return;

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

        BigDecimal equity = user.calculateEquity(totalUnrealizedPnl);
        BigDecimal marginUsed = totalPositionValue.divide(BigDecimal.valueOf(user.getLeverage()), 4, RoundingMode.HALF_UP);
        BigDecimal maintenanceMargin = marginUsed.multiply(user.getMaintenanceMarginPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));

        if (equity.compareTo(maintenanceMargin) < 0) {
            liquidationService.liquidateUser(user);
        }
    }
}