package com.siddharth.tradesim_backend.holding;

import com.siddharth.tradesim_backend.holding.model.Holding;
import com.siddharth.tradesim_backend.holding.model.dto.HoldingResponse;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HoldingService {
    private final HoldingRepository holdingRepository;
    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public List<HoldingResponse> fetchHoldings(UUID userId) {
        List<Holding> holdings = holdingRepository.findByUserId(userId);

        return holdings.stream().map(holding -> {
            Stock stock = stockRepository.findById(holding.getStockId()).orElseThrow();

            return new HoldingResponse(
                    holding.getStockId(),
                    stock.getSymbol(),
                    holding.getQuantity(),
                    stock.getLastTradedPrice()
            );
        }).toList();
    }
}