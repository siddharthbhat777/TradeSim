package com.siddharth.tradesim_backend.portfolio;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.holding.HoldingRepository;
import com.siddharth.tradesim_backend.holding.model.Holding;
import com.siddharth.tradesim_backend.order.enums.OrderType;
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
        BigDecimal tradeValue = execution.executionPrice().multiply(BigDecimal.valueOf(execution.quantity()));

        User buyer = authRepository.findById(execution.buyerId()).orElseThrow(() -> new BusinessException("User not found"));
        User seller = authRepository.findById(execution.sellerId()).orElseThrow(() -> new BusinessException("User not found"));

        Holding sellerHolding = holdingRepository.findByUserIdAndStockId(execution.sellerId(), execution.stockId()).orElseThrow(() -> new BusinessException("Seller holding not found"));

        settleBuyer(execution, buyer, tradeValue);
        settleSeller(execution, seller, sellerHolding, tradeValue);
        Holding buyerHolding = updateBuyerHolding(execution);
        holdingRepository.save(buyerHolding);

        authRepository.save(buyer);
        authRepository.save(seller);

        if (sellerHolding.getQuantity() == 0) {
            holdingRepository.delete(sellerHolding);
        } else {
            holdingRepository.save(sellerHolding);
        }
    }

    private void settleBuyer(TradeExecution execution, User buyer, BigDecimal tradeValue) {
        if (execution.buyerOrderType() == OrderType.LIMIT) {
            if (execution.buyerLimitPrice() == null) {
                throw new BusinessException("Missing buyer limit price");
            }
            BigDecimal reserved = execution.buyerLimitPrice().multiply(BigDecimal.valueOf(execution.quantity()));
            buyer.unlockFunds(reserved);
        }
        buyer.debit(tradeValue);
    }

    private void settleSeller(TradeExecution execution, User seller, Holding sellerHolding, BigDecimal tradeValue) {
        if (execution.sellerOrderType() == OrderType.LIMIT) {
            sellerHolding.unlockShares(execution.quantity());
        }
        sellerHolding.decreaseQuantity(execution.quantity());
        seller.credit(tradeValue);
    }

    private Holding updateBuyerHolding(TradeExecution execution) {
        Holding buyerHolding = holdingRepository.findByUserIdAndStockId(execution.buyerId(), execution.stockId()).orElse(null);

        if (buyerHolding == null) {
            buyerHolding = Holding.builder()
                    .userId(execution.buyerId())
                    .stockId(execution.stockId())
                    .quantity(0)
                    .lockedQuantity(0)
                    .build();
        }

        buyerHolding.increaseQuantity(execution.quantity());
        return buyerHolding;
    }
}