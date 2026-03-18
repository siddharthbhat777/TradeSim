package com.siddharth.tradesim_backend.risk.service;

import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.orderbook.OrderMatchingEngine;
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
public class LiquidationService {
    private final PositionRepository positionRepository;
    private final StockRepository stockRepository;
    private final OrderMatchingEngine orderMatchingEngine;

    public void liquidateUser(User user) {
        List<Position> positions = positionRepository.findByUserId(user.getId());

        if (positions.isEmpty()) return;

        positions.sort((position1, position2) -> {
            BigDecimal loss1 = getUnrealizedLoss(position1);
            BigDecimal loss2 = getUnrealizedLoss(position2);
            return loss2.compareTo(loss1);
        });

        for (Position position : positions) {

            if (position.getQuantity() <= 0) continue;

            while (position.getQuantity() > 0) {
                if (!shouldLiquidate(user)) {
                    return;
                }

                int sellQuantity = Math.max(1, position.getQuantity() / 10);

                Order liquidationOrder = Order.builder()
                        .userId(user.getId())
                        .stockId(position.getStockId())
                        .side(OrderSide.SELL)
                        .orderType(OrderType.MARKET)
                        .quantity(sellQuantity)
                        .remainingQuantity(sellQuantity)
                        .status(OrderStatus.OPEN)
                        .build();

                orderMatchingEngine.match(liquidationOrder);
            }
        }
    }

    private BigDecimal getUnrealizedLoss(Position position) {
        Stock stock = stockRepository.findById(position.getStockId()).orElseThrow();

        BigDecimal currentPrice = stock.getLastTradedPrice();
        BigDecimal unrealizedPnl = currentPrice.subtract(position.getAverageBuyPrice()).multiply(BigDecimal.valueOf(position.getQuantity()));

        return unrealizedPnl.min(BigDecimal.ZERO);
    }

    private boolean shouldLiquidate(User user) {
        List<Position> positions = positionRepository.findByUserId(user.getId());

        BigDecimal totalPositionValue = BigDecimal.ZERO;
        BigDecimal totalUnrealizedPnl = BigDecimal.ZERO;

        for (Position position : positions) {
            Stock stock = stockRepository.findById(position.getStockId()).orElseThrow();

            BigDecimal currentPrice = stock.getLastTradedPrice();
            BigDecimal positionValue = currentPrice.multiply(BigDecimal.valueOf(position.getQuantity()));
            BigDecimal unrealizedPnl = currentPrice.subtract(position.getAverageBuyPrice()).multiply(BigDecimal.valueOf(position.getQuantity()));

            totalPositionValue = totalPositionValue.add(positionValue);
            totalUnrealizedPnl = totalUnrealizedPnl.add(unrealizedPnl);
        }

        BigDecimal equity = user.calculateEquity(totalUnrealizedPnl);
        BigDecimal marginUsed = totalPositionValue.divide(BigDecimal.valueOf(user.getLeverage()), 4, RoundingMode.HALF_UP);
        BigDecimal maintenanceMargin = marginUsed.multiply(user.getMaintenanceMarginPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));

        return equity.compareTo(maintenanceMargin) < 0;
    }
}