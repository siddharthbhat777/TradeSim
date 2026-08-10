package com.siddharth.tradesim_backend.portfolio.service;

import com.siddharth.tradesim_backend.auth.repository.AuthRepository;
import com.siddharth.tradesim_backend.exchange.ExchangeException;
import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.exchange.model.Exchange;
import com.siddharth.tradesim_backend.forex.service.ForexService;
import com.siddharth.tradesim_backend.forex.service.FxFeeService;
import com.siddharth.tradesim_backend.ledger.LedgerService;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.portfolio.PortfolioSnapshotRepository;
import com.siddharth.tradesim_backend.portfolio.PortfolioException;
import com.siddharth.tradesim_backend.portfolio.model.PortfolioSnapshot;
import com.siddharth.tradesim_backend.portfolio.model.dto.*;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.PositionException;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.StockException;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import com.siddharth.tradesim_backend.user.UserException;
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
    private final TradingAccountService tradingAccountService;
    private final LedgerService ledgerService;
    private final ExchangeRepository exchangeRepository;
    private final ForexService forexService;
    private final FxFeeService fxFeeService;

    public PortfolioResponse fetchPortfolio(UUID userId) {
        if (!authRepository.existsById(userId)) {
            throw UserException.notFound("User not found");
        }

        TradingAccount tradingAccount = tradingAccountService.getTradingAccountByUserId(userId);

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
                throw StockException.notFound("Stock not found");
            }

            Exchange exchange = exchangeRepository.findById(stock.getExchangeId()).orElseThrow(() -> ExchangeException.notFound("Exchange not found"));

            BigDecimal totalValueInStockCurrency = stock.getLastTradedPrice().multiply(BigDecimal.valueOf(position.getQuantity()));
            BigDecimal currentValue = forexService.convert(totalValueInStockCurrency, exchange.getCurrency(), tradingAccount.getBaseCurrency());

            BigDecimal currentPriceInAccountCurrency = forexService.convert(stock.getLastTradedPrice(), exchange.getCurrency(), tradingAccount.getBaseCurrency());

            BigDecimal invested = position.getTotalInvested();
            BigDecimal unrealizedPnl = currentValue.subtract(invested);

            totalValue = totalValue.add(currentValue);
            totalInvested = totalInvested.add(invested);
            totalUnrealizedPnl = totalUnrealizedPnl.add(unrealizedPnl);
            totalRealizedPnl = totalRealizedPnl.add(position.getRealizedPnl());

            PortfolioHoldingResponse response = new PortfolioHoldingResponse(
                    position.getStockId(),
                    stock.getSymbol(),
                    position.getQuantity(),
                    position.getAverageBuyPrice(),
                    currentPriceInAccountCurrency,
                    currentValue,
                    unrealizedPnl
            );

            responses.add(response);
        }

        BigDecimal equity = tradingAccount.calculateEquity(totalValue);
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
        if (!authRepository.existsById(userId)) {
            throw UserException.notFound("User not found");
        }

        TradingAccount tradingAccount = tradingAccountService.getTradingAccountByUserId(userId);
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
            if (stock == null) {
                throw StockException.notFound("Stock not found");
            }

            Exchange exchange = exchangeRepository.findById(stock.getExchangeId()).orElseThrow(() -> ExchangeException.notFound("Exchange not found"));

            BigDecimal totalValueInStockCurrency = stock.getLastTradedPrice().multiply(BigDecimal.valueOf(position.getQuantity()));
            BigDecimal value = forexService.convert(totalValueInStockCurrency, exchange.getCurrency(), tradingAccount.getBaseCurrency());

            positionValues.put(position.getStockId(), value);

            totalValue = totalValue.add(value);
        }

        BigDecimal finalTotalValue = totalValue;

        return positions.stream()
                .map(position -> {
                    Stock stock = stockMap.get(position.getStockId());
                    if (stock == null) {
                        throw StockException.notFound("Stock not found");
                    }
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
            throw PortfolioException.conflict("Self-trading is not allowed");
        }

        if (!authRepository.existsById(execution.buyerId()) || !authRepository.existsById(execution.sellerId())) {
            throw UserException.notFound("User not found");
        }

        Stock stock = stockRepository.findById(execution.stockId()).orElseThrow(() -> StockException.notFound("Stock not found"));
        Exchange exchange = exchangeRepository.findById(stock.getExchangeId()).orElseThrow(() -> ExchangeException.notFound("Exchange not found"));

        String stockCurrency = exchange.getCurrency();

        UUID firstLockedUserId = execution.buyerId().compareTo(execution.sellerId()) <= 0 ? execution.buyerId() : execution.sellerId();
        UUID secondLockedUserId = firstLockedUserId.equals(execution.buyerId()) ? execution.sellerId() : execution.buyerId();

        TradingAccount firstLockedAccount = tradingAccountService.getTradingAccountByUserIdForUpdate(firstLockedUserId);
        TradingAccount secondLockedAccount = tradingAccountService.getTradingAccountByUserIdForUpdate(secondLockedUserId);

        TradingAccount buyerTradingAccount = execution.buyerId().equals(firstLockedUserId) ? firstLockedAccount : secondLockedAccount;
        TradingAccount sellerTradingAccount = execution.sellerId().equals(firstLockedUserId) ? firstLockedAccount : secondLockedAccount;

        String buyerCurrency = buyerTradingAccount.getBaseCurrency();
        String sellerCurrency = sellerTradingAccount.getBaseCurrency();

        BigDecimal tradeValueInStockCurrency = execution.executionPrice().multiply(BigDecimal.valueOf(execution.quantity()));

        BigDecimal exactBuyerTradeValue = forexService.convert(tradeValueInStockCurrency, stockCurrency, buyerCurrency);
        BigDecimal exactSellerTradeValue = forexService.convert(tradeValueInStockCurrency, stockCurrency, sellerCurrency);

        Position sellerPosition = positionRepository.findByUserIdAndStockId(execution.sellerId(), execution.stockId()).orElseThrow(() -> PositionException.notFound("Seller position not found"));

        settleBuyer(execution, buyerTradingAccount, exactBuyerTradeValue, stockCurrency, buyerCurrency);
        settleSeller(execution, sellerTradingAccount, sellerPosition, exactSellerTradeValue, stockCurrency, sellerCurrency);
        Position buyerPosition = updateBuyerPosition(execution, exactBuyerTradeValue);
        positionRepository.save(buyerPosition);

        tradingAccountService.saveTradingAccount(buyerTradingAccount);
        tradingAccountService.saveTradingAccount(sellerTradingAccount);

        if (sellerPosition.getQuantity() == 0 && sellerPosition.getLockedQuantity() == 0) {
            positionRepository.delete(sellerPosition);
        } else {
            positionRepository.save(sellerPosition);
        }
    }

    private void settleBuyer(TradeExecution execution, TradingAccount buyerTradingAccount, BigDecimal exactBuyerTradeValue, String stockCurrency, String buyerCurrency) {
        if (execution.buyerFundsReserved()) {
            if (execution.buyerReservationPrice() == null) {
                throw PortfolioException.conflict("Missing buyer reservation price");
            }

            BigDecimal reservedMarginInStockCurrency = execution.buyerReservationPrice()
                    .multiply(BigDecimal.valueOf(execution.quantity()))
                    .divide(BigDecimal.valueOf(buyerTradingAccount.getLeverage()), 4, RoundingMode.HALF_UP);

            BigDecimal reservedMarginInBuyerCurrency = forexService.convert(reservedMarginInStockCurrency, stockCurrency, buyerCurrency);
            BigDecimal reservedFxFee = fxFeeService.calculateConversionFee(buyerCurrency, stockCurrency, reservedMarginInBuyerCurrency);
            BigDecimal totalLocked = reservedMarginInBuyerCurrency.add(reservedFxFee);

            buyerTradingAccount.unlockFunds(totalLocked);

            if (execution.buyerOrderType() == OrderType.LIMIT) {
                ledgerService.recordBuyLimitMarginUnlock(
                        buyerTradingAccount,
                        totalLocked,
                        execution.stockId(),
                        execution.buyOrderId()
                );
            } else {
                ledgerService.recordBuyOrderMarginUnlock(
                        buyerTradingAccount,
                        totalLocked,
                        execution.stockId(),
                        execution.buyOrderId()
                );
            }
        }

        BigDecimal requiredMargin = exactBuyerTradeValue.divide(BigDecimal.valueOf(buyerTradingAccount.getLeverage()), 4, RoundingMode.HALF_UP);
        BigDecimal executionFxFee = fxFeeService.calculateConversionFee(buyerCurrency, stockCurrency, requiredMargin);

        buyerTradingAccount.debit(requiredMargin);
        if (executionFxFee.compareTo(BigDecimal.ZERO) > 0) {
            buyerTradingAccount.debit(executionFxFee);
        }

        ledgerService.recordTradeMarginDebit(
                buyerTradingAccount,
                requiredMargin,
                execution.stockId(),
                execution.buyOrderId()
        );

        if (executionFxFee.compareTo(BigDecimal.ZERO) > 0) {
            ledgerService.recordFxConversionFee(
                    buyerTradingAccount,
                    executionFxFee,
                    execution.stockId(),
                    execution.buyOrderId(),
                    null,
                    buyerCurrency,
                    stockCurrency
            );
        }

        BigDecimal loanIncrease = exactBuyerTradeValue.subtract(requiredMargin);
        if (loanIncrease.compareTo(BigDecimal.ZERO) > 0) {
            buyerTradingAccount.increaseMarginLoan(loanIncrease);
            ledgerService.recordMarginLoanIncrease(
                    buyerTradingAccount,
                    loanIncrease,
                    execution.stockId(),
                    execution.buyOrderId()
            );
        }
    }

    private void settleSeller(TradeExecution execution, TradingAccount sellerTradingAccount, Position sellerPosition, BigDecimal exactSellerTradeValue, String stockCurrency, String sellerCurrency) {
        if (execution.sellerSharesReserved()) {
            sellerPosition.unlockShares(execution.quantity());
        }

        BigDecimal costBasis = sellerPosition.getTotalInvested()
                .multiply(BigDecimal.valueOf(execution.quantity()))
                .divide(BigDecimal.valueOf(sellerPosition.getQuantity()), 4, RoundingMode.HALF_UP);

        BigDecimal pnl = exactSellerTradeValue.subtract(costBasis);

        sellerPosition.decreaseQuantity(execution.quantity());
        sellerPosition.addRealizedPnl(pnl);

        BigDecimal executionFxFee = fxFeeService.calculateConversionFee(stockCurrency, sellerCurrency, exactSellerTradeValue);
        BigDecimal netProceedsBase = exactSellerTradeValue.subtract(executionFxFee);

        BigDecimal loanToRepay = sellerTradingAccount.getMarginLoan().min(netProceedsBase);
        if (loanToRepay.compareTo(BigDecimal.ZERO) > 0) {
            sellerTradingAccount.decreaseMarginLoan(loanToRepay);
            ledgerService.recordMarginLoanRepayment(
                    sellerTradingAccount,
                    loanToRepay,
                    execution.stockId(),
                    execution.sellOrderId()
            );
            netProceedsBase = netProceedsBase.subtract(loanToRepay);
        }
        if (netProceedsBase.compareTo(BigDecimal.ZERO) > 0) {
            sellerTradingAccount.credit(netProceedsBase);
            ledgerService.recordTradeProceedsCredit(
                    sellerTradingAccount,
                    netProceedsBase,
                    execution.stockId(),
                    execution.sellOrderId()
            );
        }

        if (executionFxFee.compareTo(BigDecimal.ZERO) > 0) {
            ledgerService.recordFxConversionFee(
                    sellerTradingAccount,
                    executionFxFee,
                    execution.stockId(),
                    execution.sellOrderId(),
                    null,
                    stockCurrency,
                    sellerCurrency
            );
        }
    }

    private Position updateBuyerPosition(TradeExecution execution, BigDecimal exactBuyerTradeValue) {
        Position buyerPosition = positionRepository.findByUserIdAndStockId(execution.buyerId(), execution.stockId()).orElse(null);

        if (buyerPosition == null) {
            buyerPosition = Position.builder()
                    .userId(execution.buyerId())
                    .stockId(execution.stockId())
                    .averageBuyPrice(BigDecimal.ZERO)
                    .totalInvested(BigDecimal.ZERO)
                    .realizedPnl(BigDecimal.ZERO)
                    .quantity(0)
                    .lockedQuantity(0)
                    .build();
        }

        buyerPosition.addInvestment(exactBuyerTradeValue, execution.quantity());
        return buyerPosition;
    }
}