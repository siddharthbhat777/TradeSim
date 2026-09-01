package com.siddharth.tradesim_backend.order;

import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.order.model.dto.OrderEstimateResponse;
import com.siddharth.tradesim_backend.order.model.dto.OrderHistoryResponse;
import com.siddharth.tradesim_backend.order.model.dto.OrderRequest;
import com.siddharth.tradesim_backend.order.model.dto.OrderResponse;
import com.siddharth.tradesim_backend.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<OrderHistoryResponse>> getMyOrders(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(orderService.fetchUserOrders(user.getUserId()));
    }

    @PostMapping("estimate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderEstimateResponse> estimateOrder(@AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.estimateOrder(user.getUserId(), request));
    }

    @PostMapping("create")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderResponse> placeOrder(@AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(user.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("{orderId}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> cancelOrder(@AuthenticationPrincipal UserPrincipal user, @PathVariable UUID orderId) {
        orderService.cancelOrder(user.getUserId(), orderId);
        return ResponseEntity.noContent().build();
    }
}