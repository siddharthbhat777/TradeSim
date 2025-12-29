package com.siddharth.tradesim_backend.stock;

import com.siddharth.tradesim_backend.stock.models.Stock;
import com.siddharth.tradesim_backend.stock.models.dto.CreateStockRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("stock")
@RequiredArgsConstructor
public class StockController {
    private final StockService stockService;

    @PostMapping("add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Stock> addStock(@Valid @RequestBody CreateStockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.addStock(request));
    }

    @GetMapping("all")
    public ResponseEntity<List<Stock>> getAllStocks() {
        return ResponseEntity.ok(stockService.getAllStocks());
    }
}