package com.siddharth.tradesim_backend.position;

import com.siddharth.tradesim_backend.exchange.ExchangeException;
import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.exchange.model.Exchange;
import com.siddharth.tradesim_backend.forex.service.ForexService;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.position.model.dto.PositionResponse;
import com.siddharth.tradesim_backend.stock.StockException;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PositionService {
    private final PositionRepository positionRepository;
    private final StockRepository stockRepository;
    private final TradingAccountService tradingAccountService;
    private final ExchangeRepository exchangeRepository;
    private final ForexService forexService;

    public List<PositionResponse> fetchPositions(UUID userId) {
        TradingAccount tradingAccount = tradingAccountService.getTradingAccountByUserId(userId);
        String userCurrency = tradingAccount.getBaseCurrency();

        List<Position> positions = positionRepository.findByUserId(userId);
        List<UUID> stockIds = positions.stream().map(Position::getStockId).toList();
        Map<UUID, Stock> stockMap = stockRepository.findAllById(stockIds).stream().collect(Collectors.toMap(Stock::getId, s -> s));
        List<PositionResponse> responses = new ArrayList<>();

        for (Position position : positions) {
            Stock stock = stockMap.get(position.getStockId());
            if (stock == null) {
                throw StockException.notFound("Stock not found");
            }

            Exchange exchange = exchangeRepository.findById(stock.getExchangeId()).orElseThrow(() -> ExchangeException.notFound("Exchange not found"));

            BigDecimal currentPriceInUserCurrency = forexService.convert(stock.getLastTradedPrice(), exchange.getCurrency(), userCurrency);

            BigDecimal unrealizedPnl = currentPriceInUserCurrency.subtract(position.getAverageBuyPrice()).multiply(BigDecimal.valueOf(position.getQuantity()));

            responses.add(
                    new PositionResponse(
                            position.getStockId(),
                            stock.getSymbol(),
                            position.getQuantity(),
                            position.getLockedQuantity(),
                            position.getAverageBuyPrice(),
                            position.getRealizedPnl(),
                            unrealizedPnl
                    )
            );
        }

        return responses;
    }
}