package com.siddharth.tradesim_backend.market_index;

import com.siddharth.tradesim_backend.market_index.model.dto.AddConstituentRequest;
import com.siddharth.tradesim_backend.market_index.model.dto.CreateMarketIndexRequest;
import com.siddharth.tradesim_backend.market_index.model.dto.MarketIndexConstituentResponse;
import com.siddharth.tradesim_backend.market_index.model.dto.MarketIndexResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("indices")
@RequiredArgsConstructor
public class MarketIndexController {
    private final MarketIndexService marketIndexService;

    @GetMapping
    public ResponseEntity<List<MarketIndexResponse>> getAllIndices() {
        return ResponseEntity.ok(marketIndexService.fetchAllIndices());
    }

    @GetMapping("exchange/{exchangeId}")
    public ResponseEntity<List<MarketIndexResponse>> getIndicesByExchange(@PathVariable UUID exchangeId) {
        return ResponseEntity.ok(marketIndexService.fetchIndicesByExchange(exchangeId));
    }

    @GetMapping("{indexId}/constituents")
    public ResponseEntity<List<MarketIndexConstituentResponse>> getConstituents(@PathVariable UUID indexId) {
        return ResponseEntity.ok(marketIndexService.fetchConstituents(indexId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MarketIndexResponse> createIndex(@Valid @RequestBody CreateMarketIndexRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(marketIndexService.createIndex(request));
    }

    @PostMapping("{indexId}/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MarketIndexResponse> initializeIndex(@PathVariable UUID indexId) {
        return ResponseEntity.ok(marketIndexService.initializeIndex(indexId));
    }

    @PostMapping("{indexId}/constituents")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addConstituent(@PathVariable UUID indexId, @Valid @RequestBody AddConstituentRequest request) {
        marketIndexService.addConstituent(indexId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("{indexId}/constituents/{stockId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeConstituent(@PathVariable UUID indexId, @PathVariable UUID stockId) {
        marketIndexService.removeConstituent(indexId, stockId);
        return ResponseEntity.noContent().build();
    }
}