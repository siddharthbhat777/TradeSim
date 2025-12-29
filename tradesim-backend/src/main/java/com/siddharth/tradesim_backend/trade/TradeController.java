package com.siddharth.tradesim_backend.trade;

import com.siddharth.tradesim_backend.trade.models.dto.BuyTradeRequest;
import com.siddharth.tradesim_backend.trade.models.dto.TradeResponse;
import com.siddharth.tradesim_backend.trade.services.TradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("trades")
@RequiredArgsConstructor
public class TradeController {
    private final TradeService tradeService;

    @PostMapping("buy/{userId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TradeResponse> placeBuyOrder(@PathVariable UUID userId, @Valid @RequestBody BuyTradeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tradeService.placeBuyOrder(userId, request));
    }
}