package com.siddharth.tradesim_backend.position;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.exchange.model.Exchange;
import com.siddharth.tradesim_backend.forex.service.ForexService;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.position.model.dto.PositionResponse;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private TradingAccountService tradingAccountService;

    @Mock
    private ExchangeRepository exchangeRepository;

    @Mock
    private ForexService forexService;

    @InjectMocks
    private PositionService positionService;

    @Test
    void shouldReturnPositionResponsesWhenStocksExist() {
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();

        TradingAccount tradingAccount = TradingAccount.builder()
                .userId(userId)
                .baseCurrency("INR")
                .build();

        Position position = Position.builder()
                .userId(userId)
                .stockId(stockId)
                .quantity(10)
                .lockedQuantity(2)
                .averageBuyPrice(BigDecimal.valueOf(90))
                .realizedPnl(BigDecimal.valueOf(25))
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .symbol("AAPL")
                .exchangeId(exchangeId)
                .lastTradedPrice(BigDecimal.valueOf(100))
                .build();

        Exchange exchange = Exchange.builder()
                .id(exchangeId)
                .currency("USD")
                .build();

        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);
        when(positionRepository.findByUserId(userId)).thenReturn(List.of(position));
        when(stockRepository.findAllById(List.of(stockId))).thenReturn(List.of(stock));
        when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));
        when(forexService.convert(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<PositionResponse> responses = positionService.fetchPositions(userId);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().symbol()).isEqualTo("AAPL");
        assertThat(responses.getFirst().unrealizedPnl()).isEqualByComparingTo("100");
    }

    @Test
    void shouldThrowWhenPositionStockIsMissing() {
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        TradingAccount tradingAccount = TradingAccount.builder()
                .userId(userId)
                .baseCurrency("INR")
                .build();

        Position position = Position.builder()
                .userId(userId)
                .stockId(stockId)
                .quantity(5)
                .lockedQuantity(0)
                .averageBuyPrice(BigDecimal.valueOf(90))
                .realizedPnl(BigDecimal.ZERO)
                .build();

        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);
        when(positionRepository.findByUserId(userId)).thenReturn(List.of(position));
        when(stockRepository.findAllById(List.of(stockId))).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> positionService.fetchPositions(userId));
    }
}