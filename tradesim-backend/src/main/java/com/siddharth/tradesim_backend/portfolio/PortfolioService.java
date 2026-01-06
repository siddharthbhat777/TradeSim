package com.siddharth.tradesim_backend.portfolio;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.holding.HoldingRepository;
import com.siddharth.tradesim_backend.holding.model.Holding;
import com.siddharth.tradesim_backend.portfolio.dto.PortfolioHoldingResponse;
import com.siddharth.tradesim_backend.portfolio.dto.PortfolioResponse;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortfolioService {
    private final HoldingRepository holdingRepository;
    private final StockRepository stockRepository;

    public PortfolioResponse fetchPortfolio(UUID userId) {
        List<Holding> holdings = holdingRepository.findByUserId(userId);
        List<PortfolioHoldingResponse> responses = new ArrayList<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        for (Holding holding : holdings) {
            Stock stock = stockRepository.findById(holding.getStockId()).orElseThrow(() -> new BusinessException("Stock not found"));
            BigDecimal currentPrice = stock.getCurrentPrice();
            BigDecimal currentValue = currentPrice.multiply(BigDecimal.valueOf(holding.getQuantity()));

            totalValue = totalValue.add(currentValue);

            PortfolioHoldingResponse response = new PortfolioHoldingResponse(
                    holding.getStockId(),
                    stock.getSymbol(),
                    holding.getQuantity(),
                    currentPrice,
                    currentValue
            );

            responses.add(response);
        }

        return new PortfolioResponse(responses, totalValue);
    }
}