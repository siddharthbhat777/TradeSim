package com.siddharth.tradesim_backend.scheduler;

import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.order.service.OrderLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DayOrderExpiryScheduler {
    private final OrderRepository orderRepository;
    private final OrderLifecycleService orderLifecycleService;

    @Scheduled(fixedDelay = 30000)
    public void cancelExpiredDayOrders() {
        List<Order> expiredOrders = orderRepository.findByStatusInAndExpiresAtLessThanEqual(
                List.of(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED),
                Instant.now()
        );

        for (Order order : expiredOrders) {
            try {
                orderLifecycleService.cancelOrder(order);
            } catch (Exception e) {
                log.error("Failed to expire order {}", order.getId(), e);
            }
        }
    }
}