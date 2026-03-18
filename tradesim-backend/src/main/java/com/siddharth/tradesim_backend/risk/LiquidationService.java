package com.siddharth.tradesim_backend.risk;

import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.orderbook.OrderMatchingEngine;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LiquidationService {
    private final PositionRepository positionRepository;
    private final OrderMatchingEngine orderMatchingEngine;

    public void liquidateUser(User user) {
        List<Position> positions = positionRepository.findByUserId(user.getId());

        for (Position position : positions) {

            if (position.getQuantity() <= 0) continue;

            Order liquidationOrder = Order.builder()
                    .userId(user.getId())
                    .stockId(position.getStockId())
                    .side(OrderSide.SELL)
                    .orderType(OrderType.MARKET)
                    .quantity(position.getQuantity())
                    .remainingQuantity(position.getQuantity())
                    .status(OrderStatus.OPEN)
                    .build();

            orderMatchingEngine.match(liquidationOrder);
        }
    }
}