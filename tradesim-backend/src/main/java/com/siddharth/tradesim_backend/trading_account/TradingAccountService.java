package com.siddharth.tradesim_backend.trading_account;

import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import com.siddharth.tradesim_backend.trading_account.model.dto.TradingAccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TradingAccountService {
    private static final int DEFAULT_LEVERAGE = 5;
    private static final BigDecimal DEFAULT_MAINTENANCE_MARGIN_PERCENT = BigDecimal.valueOf(25);

    private final TradingAccountRepository tradingAccountRepository;

    @Transactional
    public TradingAccount createTradingAccountForUser(UUID userId) {
        return createTradingAccountForUser(userId, "INR");
    }

    @Transactional
    public TradingAccount createTradingAccountForUser(UUID userId, String baseCurrency) {
        if (tradingAccountRepository.existsByUserId(userId)) {
            throw TradingAccountException.conflict("Trading account already exists for this user");
        }

        TradingAccount tradingAccount = TradingAccount.builder()
                .userId(userId)
                .baseCurrency(baseCurrency != null ? baseCurrency : "INR")
                .marginLoan(BigDecimal.ZERO)
                .leverage(DEFAULT_LEVERAGE)
                .maintenanceMarginPercent(DEFAULT_MAINTENANCE_MARGIN_PERCENT)
                .build();

        return tradingAccountRepository.save(tradingAccount);
    }

    @Transactional(readOnly = true)
    public TradingAccount getTradingAccountByUserId(UUID userId) {
        return tradingAccountRepository.findByUserId(userId).orElseThrow(() -> TradingAccountException.notFound("Trading account not found"));
    }

    @Transactional
    public TradingAccount getTradingAccountByUserIdForUpdate(UUID userId) {
        return tradingAccountRepository.findByUserIdForUpdate(userId).orElseThrow(() -> TradingAccountException.notFound("Trading account not found"));
    }

    @Transactional
    public void saveTradingAccount(TradingAccount tradingAccount) {
        tradingAccountRepository.save(tradingAccount);
    }

    @Transactional(readOnly = true)
    public TradingAccountResponse fetchMyTradingAccount(UUID userId) {
        TradingAccount tradingAccount = getTradingAccountByUserId(userId);
        return toResponse(tradingAccount);
    }

    private TradingAccountResponse toResponse(TradingAccount tradingAccount) {
        return new TradingAccountResponse(
                tradingAccount.getId(),
                tradingAccount.getUserId(),
                tradingAccount.getBaseCurrency(),
                tradingAccount.getMarginLoan(),
                tradingAccount.getLeverage(),
                tradingAccount.getMaintenanceMarginPercent(),
                tradingAccount.getCreatedAt(),
                tradingAccount.getUpdatedAt()
        );
    }
}