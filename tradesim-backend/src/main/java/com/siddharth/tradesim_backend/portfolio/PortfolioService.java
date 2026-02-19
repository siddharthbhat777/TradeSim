package com.siddharth.tradesim_backend.portfolio;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.holding.HoldingRepository;
import com.siddharth.tradesim_backend.holding.model.Holding;
import com.siddharth.tradesim_backend.portfolio.dto.PortfolioHoldingResponse;
import com.siddharth.tradesim_backend.portfolio.dto.PortfolioResponse;
import com.siddharth.tradesim_backend.portfolio.dto.TradeExecution;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortfolioService {
    private final HoldingRepository holdingRepository;
    private final StockRepository stockRepository;
    private final AuthRepository authRepository;

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

    @Transactional
    public void settleTrade(TradeExecution execution) {
        BigDecimal tradeValue = execution.price().multiply(BigDecimal.valueOf(execution.quantity()));
        addHolding(execution.buyerId(), execution.stockId(), execution.quantity());
        removeHolding(execution.sellerId(), execution.stockId(), execution.quantity());

        debitUser(execution.buyerId(), tradeValue);
        creditUser(execution.sellerId(), tradeValue);
    }

    private void addHolding(UUID userId, UUID stockId, int quantity) {
        Holding holding = holdingRepository.findByUserIdAndStockId(userId, stockId).orElse(null);

        if (holding == null) {
            holding = Holding.builder()
                    .userId(userId)
                    .stockId(stockId)
                    .quantity(quantity)
                    .build();
        } else {
            holding.setQuantity(holding.getQuantity() + quantity);
        }

        holdingRepository.save(holding);
    }

    private void removeHolding(UUID userId, UUID stockId, int quantity) {
        Holding holding = holdingRepository.findByUserIdAndStockId(userId, stockId).orElseThrow(() -> new BusinessException("Holding not found"));

        if (holding.getQuantity() < quantity) {
            throw new BusinessException("Insufficient shares to sell");
        }

        holding.setQuantity(holding.getQuantity() - quantity);

        if (holding.getQuantity() == 0) {
            holdingRepository.delete(holding);
        } else {
            holdingRepository.save(holding);
        }
    }

    private void debitUser(UUID userId, BigDecimal amount) {
        User user = authRepository.findById(userId).orElseThrow(() -> new BusinessException("User not found"));
        user.debit(amount);
        authRepository.save(user);
    }

    private void creditUser(UUID userId, BigDecimal amount) {
        User user = authRepository.findById(userId).orElseThrow(() -> new BusinessException("User not found"));
        user.credit(amount);
        authRepository.save(user);
    }
}