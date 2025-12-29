package com.siddharth.tradesim_backend.trade.services;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.models.User;
import com.siddharth.tradesim_backend.trade.TradeRepository;
import com.siddharth.tradesim_backend.trade.enums.Status;
import com.siddharth.tradesim_backend.trade.enums.Type;
import com.siddharth.tradesim_backend.trade.models.Trade;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TradeExecutionService {

    private final TradeRepository tradeRepository;
    private final AuthRepository authRepository;

    @Transactional
    public void executeTrade(
            Trade trade,
            User user,
            BigDecimal executionPrice
    ) {
        if (trade.getStatus() == Status.EXECUTED) {
            return;
        }

        BigDecimal totalCost = executionPrice.multiply(BigDecimal.valueOf(trade.getQuantity()));

        if (trade.getType() == Type.BUY) {
            if (user.getBalance().compareTo(totalCost) < 0) {
                trade.setStatus(Status.FAILED);
                tradeRepository.save(trade);
                return;
            }
            user.setBalance(user.getBalance().subtract(totalCost));
        }

        trade.setPriceAtExecution(executionPrice);
        trade.setStatus(Status.EXECUTED);

        authRepository.save(user);
        tradeRepository.save(trade);
    }
}