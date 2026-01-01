package com.siddharth.tradesim_backend.trade;

import com.siddharth.tradesim_backend.auth.models.UserPrincipal;
import com.siddharth.tradesim_backend.trade.models.dto.TradeRequest;
import com.siddharth.tradesim_backend.trade.models.dto.TradeResponse;
import com.siddharth.tradesim_backend.trade.services.TradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("trades")
@RequiredArgsConstructor
public class TradeController {
    private final TradeService tradeService;

    @PostMapping("order")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TradeResponse> placeOrder(@AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody TradeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tradeService.placeOrder(user.getUserId(), request));
    }

    @PutMapping("{tradeId}/cancel")
    public ResponseEntity<TradeResponse> cancelTrade(@PathVariable UUID tradeId, @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(
                tradeService.cancelTrade(tradeId, user.getUserId())
        );
    }
}