package com.siddharth.tradesim_backend.risk;

import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.exchange.model.Exchange;
import com.siddharth.tradesim_backend.forex.service.ForexService;
import com.siddharth.tradesim_backend.order.orderbook.OrderBook;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookManager;
import com.siddharth.tradesim_backend.order.orderbook.OrderMatchingEngine;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.order.service.OrderLifecycleService;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.risk.service.LiquidationService;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import com.siddharth.tradesim_backend.wallet.model.Wallet;
import com.siddharth.tradesim_backend.wallet.model.WalletBucket;
import com.siddharth.tradesim_backend.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiquidationServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private TradingAccountService tradingAccountService;

    @Mock
    private WalletService walletService;

    @Mock
    private OrderMatchingEngine orderMatchingEngine;

    @Mock
    private OrderBookManager orderBookManager;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderLifecycleService orderLifecycleService;

    @Mock
    private ExchangeRepository exchangeRepository;

    @Mock
    private ForexService forexService;

    @InjectMocks
    private LiquidationService liquidationService;

    @Test
    @SuppressWarnings("unchecked")
    void shouldCancelRemainingQuantityForIocLiquidationOrders() {
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        UUID positionId = UUID.randomUUID();

        Position position = Position.builder()
                .id(positionId)
                .userId(userId)
                .stockId(stockId)
                .quantity(20)
                .lockedQuantity(0)
                .averageBuyPrice(BigDecimal.valueOf(150))
                .realizedPnl(BigDecimal.ZERO)
                .build();

        Stock stock = Stock.builder()
                .id(stockId)
                .exchangeId(UUID.randomUUID())
                .lastTradedPrice(BigDecimal.valueOf(100))
                .build();

        TradingAccount tradingAccount = TradingAccount.builder()
                .userId(userId)
                .baseCurrency("INR")
                .marginLoan(BigDecimal.valueOf(3000))
                .leverage(5)
                .maintenanceMarginPercent(BigDecimal.valueOf(50))
                .build();

        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .buckets(List.of(WalletBucket.builder().currency("INR").balance(BigDecimal.ZERO).build()))
                .build();

        Exchange exchange = Exchange.builder().currency("USD").build();

        when(positionRepository.findByUserId(userId)).thenReturn(new ArrayList<>(List.of(position)));
        when(positionRepository.findById(positionId)).thenReturn(Optional.of(position), Optional.empty());
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);
        when(walletService.getWalletByUserId(userId)).thenReturn(wallet);
        when(exchangeRepository.findById(stock.getExchangeId())).thenReturn(Optional.of(exchange));
        when(forexService.convert(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(orderBookManager.withLock(eq(stockId), ArgumentMatchers.<Function<OrderBook, Boolean>>any())).thenReturn(true);
        when(orderMatchingEngine.match(any())).thenAnswer(invocation -> {
            com.siddharth.tradesim_backend.order.model.Order order = invocation.getArgument(0);
            order.execute(1);
            return null;
        });

        liquidationService.liquidateUser(userId);

        verify(orderRepository).save(any(com.siddharth.tradesim_backend.order.model.Order.class));
        verify(orderLifecycleService).cancelOrder(any(com.siddharth.tradesim_backend.order.model.Order.class));
    }
}