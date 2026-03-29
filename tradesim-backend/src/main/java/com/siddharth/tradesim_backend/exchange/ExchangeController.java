package com.siddharth.tradesim_backend.exchange;

import com.siddharth.tradesim_backend.exchange.model.dto.ChangeExchangeStatusRequest;
import com.siddharth.tradesim_backend.exchange.model.dto.CreateExchangeRequest;
import com.siddharth.tradesim_backend.exchange.model.dto.ExchangeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("exchanges")
@RequiredArgsConstructor
public class ExchangeController {
    private final ExchangeService exchangeService;

    @GetMapping
    public ResponseEntity<List<ExchangeResponse>> getExchanges() {
        return ResponseEntity.ok(exchangeService.fetchExchanges());
    }

    @GetMapping("{exchangeId}")
    public ResponseEntity<ExchangeResponse> getExchange(@PathVariable UUID exchangeId) {
        return ResponseEntity.ok(exchangeService.fetchExchange(exchangeId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExchangeResponse> createExchange(@Valid @RequestBody CreateExchangeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exchangeService.createExchange(request));
    }

    @PutMapping("{exchangeId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExchangeResponse> changeStatus(@PathVariable UUID exchangeId, @Valid @RequestBody ChangeExchangeStatusRequest request) {
        return ResponseEntity.ok(exchangeService.changeStatus(exchangeId, request.status()));
    }
}