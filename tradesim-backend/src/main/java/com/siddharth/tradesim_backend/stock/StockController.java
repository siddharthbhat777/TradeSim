package com.siddharth.tradesim_backend.stock;

import com.siddharth.tradesim_backend.stock.model.dto.CreateStockRequest;
import com.siddharth.tradesim_backend.stock.model.dto.StockResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("stocks")
@RequiredArgsConstructor
public class StockController {
    private final StockService stockService;

    @GetMapping
    public ResponseEntity<List<StockResponse>> getStocks() {
        return ResponseEntity.ok(stockService.fetchStocks());
    }

    @PostMapping("add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockResponse> addStock(@Valid @RequestBody CreateStockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.addStock(request));
    }
}