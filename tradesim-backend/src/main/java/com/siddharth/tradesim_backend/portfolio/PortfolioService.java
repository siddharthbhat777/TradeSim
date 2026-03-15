package com.siddharth.tradesim_backend.portfolio;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.portfolio.dto.PortfolioHoldingResponse;
import com.siddharth.tradesim_backend.portfolio.dto.PortfolioResponse;
import com.siddharth.tradesim_backend.portfolio.dto.TradeExecution;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {
    private final PositionRepository positionRepository;
    private final StockRepository stockRepository;
    private final AuthRepository authRepository;

    public PortfolioResponse fetchPortfolio(UUID userId) {
        User user = authRepository.findById(userId).orElseThrow(() -> new BusinessException("User not found"));
        List<Position> positions = positionRepository.findByUserId(userId);
        List<UUID> stockIds = positions.stream().map(Position::getStockId).toList();
        List<Stock> stocks = stockRepository.findAllById(stockIds);
        Map<UUID, Stock> stockMap = stocks.stream().collect(Collectors.toMap(Stock::getId, s -> s));

        List<PortfolioHoldingResponse> responses = new ArrayList<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalUnrealizedPnl = BigDecimal.ZERO;
        BigDecimal totalRealizedPnl = BigDecimal.ZERO;
        BigDecimal equity = user.calculateEquity(totalUnrealizedPnl);

        for (Position position : positions) {
            Stock stock = stockMap.get(position.getStockId());
            if (stock == null) {
                throw new BusinessException("Stock not found");
            }

            BigDecimal currentPrice = stock.getLastTradedPrice();
            BigDecimal currentValue = currentPrice.multiply(BigDecimal.valueOf(position.getQuantity()));
            BigDecimal unrealizedPnl = currentPrice.subtract(position.getAverageBuyPrice()).multiply(BigDecimal.valueOf(position.getQuantity()));
            BigDecimal invested = position.getAverageBuyPrice().multiply(BigDecimal.valueOf(position.getQuantity()));

            totalValue = totalValue.add(currentValue);
            totalInvested = totalInvested.add(invested);
            totalUnrealizedPnl = totalUnrealizedPnl.add(unrealizedPnl);
            totalRealizedPnl = totalRealizedPnl.add(position.getRealizedPnl());

            PortfolioHoldingResponse response = new PortfolioHoldingResponse(
                    position.getStockId(),
                    stock.getSymbol(),
                    position.getQuantity(),
                    position.getAverageBuyPrice(),
                    currentPrice,
                    currentValue,
                    unrealizedPnl
            );

            responses.add(response);
        }

        BigDecimal totalPnl = totalRealizedPnl.add(totalUnrealizedPnl);
        return new PortfolioResponse(
                responses,
                totalValue,
                totalInvested,
                totalUnrealizedPnl,
                totalRealizedPnl,
                totalPnl,
                equity
        );
    }

    @Transactional
    public void settleTrade(TradeExecution execution) {
        if (execution.buyerId().equals(execution.sellerId())) {
            throw new BusinessException("Self-trading is not allowed");
        }
        BigDecimal tradeValue = execution.executionPrice().multiply(BigDecimal.valueOf(execution.quantity()));

        User buyer = authRepository.findById(execution.buyerId()).orElseThrow(() -> new BusinessException("User not found"));
        User seller = authRepository.findById(execution.sellerId()).orElseThrow(() -> new BusinessException("User not found"));

        Position sellerPosition = positionRepository.findByUserIdAndStockId(execution.sellerId(), execution.stockId()).orElseThrow(() -> new BusinessException("Seller position not found"));

        settleBuyer(execution, buyer, tradeValue);
        settleSeller(execution, seller, sellerPosition, tradeValue);
        Position buyerPosition = updateBuyerPosition(execution);
        positionRepository.save(buyerPosition);

        authRepository.save(buyer);
        authRepository.save(seller);

        if (sellerPosition.getQuantity() == 0 && sellerPosition.getLockedQuantity() == 0) {
            positionRepository.delete(sellerPosition);
        } else {
            positionRepository.save(sellerPosition);
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

    private void settleSeller(TradeExecution execution, User seller, Position sellerPosition, BigDecimal tradeValue) {
        if (execution.sellerOrderType() == OrderType.LIMIT) {
            sellerPosition.unlockShares(execution.quantity());
        }
        BigDecimal executionPrice = execution.executionPrice();
        BigDecimal averagePrice = sellerPosition.getAverageBuyPrice();

        BigDecimal pnl = executionPrice.subtract(averagePrice).multiply(BigDecimal.valueOf(execution.quantity()));
        sellerPosition.decreaseQuantity(execution.quantity());
        sellerPosition.addRealizedPnl(pnl);
        seller.credit(tradeValue);
    }

    private Position updateBuyerPosition(TradeExecution execution) {
        Position buyerPosition = positionRepository.findByUserIdAndStockId(execution.buyerId(), execution.stockId()).orElse(null);

        if (buyerPosition == null) {
            buyerPosition = Position.builder()
                    .userId(execution.buyerId())
                    .stockId(execution.stockId())
                    .averageBuyPrice(execution.executionPrice())
                    .realizedPnl(BigDecimal.ZERO)
                    .quantity(0)
                    .lockedQuantity(0)
                    .build();
        }

        buyerPosition.updateAverageBuyPrice(execution.executionPrice(), execution.quantity());
        buyerPosition.increaseQuantity(execution.quantity());
        return buyerPosition;
    }
}