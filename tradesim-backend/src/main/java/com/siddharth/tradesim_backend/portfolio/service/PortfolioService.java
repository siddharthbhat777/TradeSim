package com.siddharth.tradesim_backend.portfolio.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.portfolio.PortfolioSnapshotRepository;
import com.siddharth.tradesim_backend.portfolio.model.PortfolioSnapshot;
import com.siddharth.tradesim_backend.portfolio.model.dto.*;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {
    private final PositionRepository positionRepository;
    private final StockRepository stockRepository;
    private final AuthRepository authRepository;
    private final PortfolioSnapshotRepository portfolioSnapshotRepository;

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

        BigDecimal equity = user.calculateEquity(totalUnrealizedPnl);
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

    @Transactional(readOnly = true)
    public List<PortfolioHistoryResponse> fetchPortfolioHistory(UUID userId) {
        List<PortfolioSnapshot> portfolioSnapshots = portfolioSnapshotRepository.findByUserIdOrderBySnapshotDate(userId);

        return portfolioSnapshots.stream()
                .map(snapshot -> new PortfolioHistoryResponse(
                        snapshot.getSnapshotDate(),
                        snapshot.getTotalValue(),
                        snapshot.getUnrealizedPnl(),
                        snapshot.getRealizedPnl(),
                        snapshot.getEquity()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PortfolioExposureResponse> fetchExposure(UUID userId) {
        List<Position> positions = positionRepository.findByUserId(userId);

        if (positions.isEmpty()) {
            return List.of();
        }

        List<UUID> stockIds = positions.stream().map(Position::getStockId).toList();
        Map<UUID, Stock> stockMap = stockRepository.findAllById(stockIds).stream().collect(Collectors.toMap(Stock::getId, s -> s));
        BigDecimal totalValue = BigDecimal.ZERO;
        Map<UUID, BigDecimal> positionValues = new HashMap<>();

        for (Position position : positions) {
            Stock stock = stockMap.get(position.getStockId());
            BigDecimal currentPrice = stock.getLastTradedPrice();
            BigDecimal value = currentPrice.multiply(BigDecimal.valueOf(position.getQuantity()));

            positionValues.put(position.getStockId(), value);

            totalValue = totalValue.add(value);
        }

        BigDecimal finalTotalValue = totalValue;

        return positions.stream()
                .map(position -> {
                    Stock stock = stockMap.get(position.getStockId());
                    BigDecimal value = positionValues.get(position.getStockId());
                    BigDecimal exposurePercent = BigDecimal.ZERO;

                    if (finalTotalValue.compareTo(BigDecimal.ZERO) > 0) {
                        exposurePercent = value
                                .multiply(BigDecimal.valueOf(100))
                                .divide(finalTotalValue, 4, RoundingMode.HALF_UP);
                    }

                    return new PortfolioExposureResponse(
                            position.getStockId(),
                            stock.getSymbol(),
                            value,
                            exposurePercent
                    );
                })
                .toList();
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