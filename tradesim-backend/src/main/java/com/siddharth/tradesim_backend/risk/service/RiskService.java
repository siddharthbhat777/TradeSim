package com.siddharth.tradesim_backend.risk.service;

import com.siddharth.tradesim_backend.auth.repository.AuthRepository;
import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.exchange.ExchangeException;
import com.siddharth.tradesim_backend.exchange.model.Exchange;
import com.siddharth.tradesim_backend.forex.service.ForexService;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.risk.dto.RiskResponse;
import com.siddharth.tradesim_backend.risk.enums.RiskLevel;
import com.siddharth.tradesim_backend.risk.RiskException;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.StockException;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import com.siddharth.tradesim_backend.user.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RiskService {
    private final PositionRepository positionRepository;
    private final StockRepository stockRepository;
    private final AuthRepository authRepository;
    private final TradingAccountService tradingAccountService;
    private final LiquidationService liquidationService;
    private final ExchangeRepository exchangeRepository;
    private final ForexService forexService;

    public void validateBuyOrder(TradingAccount tradingAccount, BigDecimal orderValueUserCurrency) {
        BigDecimal requiredMargin = orderValueUserCurrency.divide(BigDecimal.valueOf(tradingAccount.getLeverage()), 4, RoundingMode.HALF_UP);

        if (tradingAccount.getAvailableBalance().compareTo(requiredMargin) < 0) {
            throw RiskException.conflict("Insufficient margin");
        }
    }

    public void checkLiquidation(UUID userId) {
        authRepository.findById(userId).orElseThrow(() -> UserException.notFound("User not found"));
        TradingAccount tradingAccount = tradingAccountService.getTradingAccountByUserId(userId);
        RiskResponse risk = calculateRisk(userId, tradingAccount);

        if (risk.isUnderLiquidation()) {
            liquidationService.liquidateUser(userId);
        }
    }

    public RiskResponse getUserRisk(UUID userId) {
        authRepository.findById(userId).orElseThrow(() -> UserException.notFound("User not found"));
        TradingAccount tradingAccount = tradingAccountService.getTradingAccountByUserId(userId);
        return calculateRisk(userId, tradingAccount);
    }

    private RiskResponse calculateRisk(UUID userId, TradingAccount tradingAccount) {
        List<Position> positions = positionRepository.findByUserId(userId);

        BigDecimal totalPositionValue = BigDecimal.ZERO;
        BigDecimal totalUnrealizedPnl = BigDecimal.ZERO;

        for (Position position : positions) {
            Stock stock = stockRepository.findById(position.getStockId()).orElseThrow(() -> StockException.notFound("Stock not found"));
            Exchange exchange = exchangeRepository.findById(stock.getExchangeId()).orElseThrow(() -> ExchangeException.notFound("Exchange not found"));
            String stockCurrency = exchange.getCurrency();

            BigDecimal currentPriceInUserCurrency = forexService.convert(stock.getLastTradedPrice(), stockCurrency, tradingAccount.getBaseCurrency());

            BigDecimal positionValue = currentPriceInUserCurrency.multiply(BigDecimal.valueOf(position.getQuantity()));
            BigDecimal unrealizedPnl = currentPriceInUserCurrency.subtract(position.getAverageBuyPrice()).multiply(BigDecimal.valueOf(position.getQuantity()));

            totalPositionValue = totalPositionValue.add(positionValue);
            totalUnrealizedPnl = totalUnrealizedPnl.add(unrealizedPnl);
        }

        BigDecimal equity = tradingAccount.calculateEquity(totalPositionValue);
        BigDecimal marginUsed = BigDecimal.ZERO;
        if (totalPositionValue.compareTo(BigDecimal.ZERO) > 0) {
            marginUsed = totalPositionValue.divide(BigDecimal.valueOf(tradingAccount.getLeverage()), 4, RoundingMode.HALF_UP);
        }

        BigDecimal maintenanceMargin = marginUsed.multiply(tradingAccount.getMaintenanceMarginPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal marginRatio = BigDecimal.ZERO;
        if (maintenanceMargin.compareTo(BigDecimal.ZERO) > 0) {
            marginRatio = equity.divide(maintenanceMargin, 4, RoundingMode.HALF_UP);
        }

        RiskLevel riskLevel;
        if (maintenanceMargin.compareTo(BigDecimal.ZERO) == 0) {
            riskLevel = RiskLevel.SAFE;
        } else if (marginRatio.compareTo(BigDecimal.ONE) < 0) {
            riskLevel = RiskLevel.LIQUIDATION;
        } else if (marginRatio.compareTo(BigDecimal.valueOf(2)) < 0) {
            riskLevel = RiskLevel.WARNING;
        } else {
            riskLevel = RiskLevel.SAFE;
        }

        boolean isUnderLiquidation = equity.compareTo(maintenanceMargin) < 0;

        return new RiskResponse(
                equity,
                marginUsed,
                maintenanceMargin,
                totalUnrealizedPnl,
                marginRatio,
                riskLevel,
                isUnderLiquidation
        );
    }
}