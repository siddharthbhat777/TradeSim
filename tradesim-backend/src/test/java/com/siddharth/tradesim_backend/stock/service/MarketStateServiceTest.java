package com.siddharth.tradesim_backend.stock.service;

import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.orderbook.OrderBook;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookEntry;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookManager;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketStateServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private OrderBookManager orderBookManager;

    @InjectMocks
    private MarketStateService marketStateService;

    @Test
    void shouldCalculateMidPriceWhenBidAndAskExist() {
        UUID stockId = UUID.randomUUID();

        OrderBook orderBook = new OrderBook();

        orderBook.addOrder(buyEntry(BigDecimal.valueOf(100)));
        orderBook.addOrder(sellEntry(BigDecimal.valueOf(110)));

        mockWithLock(stockId, orderBook);

        BigDecimal price = marketStateService.calculateIndicativePrice(stockId);

        assertThat(price).isEqualByComparingTo("105.0000");
    }

    @Test
    void shouldReturnBidPriceWhenOnlyBidExists() {
        UUID stockId = UUID.randomUUID();

        OrderBook orderBook = new OrderBook();

        orderBook.addOrder(buyEntry(BigDecimal.valueOf(120)));

        mockWithLock(stockId, orderBook);

        BigDecimal price = marketStateService.calculateIndicativePrice(stockId);

        assertThat(price).isEqualByComparingTo("120");
    }

    @Test
    void shouldReturnAskPriceWhenOnlyAskExists() {
        UUID stockId = UUID.randomUUID();

        OrderBook orderBook = new OrderBook();

        orderBook.addOrder(sellEntry(BigDecimal.valueOf(130)));

        mockWithLock(stockId, orderBook);

        BigDecimal price = marketStateService.calculateIndicativePrice(stockId);

        assertThat(price).isEqualByComparingTo("130");
    }

    @Test
    void shouldReturnLastTradedPriceWhenNoOrdersExist() {
        UUID stockId = UUID.randomUUID();

        OrderBook orderBook = new OrderBook();

        Stock stock = Stock.builder()
                .id(stockId)
                .lastTradedPrice(BigDecimal.valueOf(95))
                .build();

        mockWithLock(stockId, orderBook);
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        BigDecimal price = marketStateService.calculateIndicativePrice(stockId);

        assertThat(price).isEqualByComparingTo("95");
    }

    @Test
    void shouldUpdateStockOnFirstTradeOfDay() {
        UUID stockId = UUID.randomUUID();

        Stock stock = Stock.builder()
                .id(stockId)
                .totalVolume(0L)
                .build();

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        marketStateService.recordTrade(stockId, BigDecimal.valueOf(100), 10);

        assertThat(stock.getDayOpen()).isEqualByComparingTo("100");
        assertThat(stock.getDayHigh()).isEqualByComparingTo("100");
        assertThat(stock.getDayLow()).isEqualByComparingTo("100");
        assertThat(stock.getDayVolume()).isEqualTo(10L);
        assertThat(stock.getLastTradedPrice()).isEqualByComparingTo("100");
        assertThat(stock.getTotalVolume()).isEqualTo(10L);
        verify(stockRepository).save(stock);
    }

    @Test
    void shouldUpdateHighLowAndVolumeForSameTradingDay() {
        UUID stockId = UUID.randomUUID();

        Stock stock = Stock.builder()
                .id(stockId)
                .lastTradingDate(LocalDate.now())
                .dayHigh(BigDecimal.valueOf(100))
                .dayLow(BigDecimal.valueOf(90))
                .dayVolume(20L)
                .totalVolume(50L)
                .build();

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        marketStateService.recordTrade(stockId, BigDecimal.valueOf(110), 5);

        assertThat(stock.getDayHigh()).isEqualByComparingTo("110");
        assertThat(stock.getDayVolume()).isEqualTo(25L);
        assertThat(stock.getTotalVolume()).isEqualTo(55L);
        verify(stockRepository).save(stock);
    }

    @Test
    void shouldReturnTrueWhenPriceWithinBand() {
        UUID stockId = UUID.randomUUID();

        Stock stock = Stock.builder()
                .id(stockId)
                .lastTradedPrice(BigDecimal.valueOf(100))
                .priceBandPercent(BigDecimal.valueOf(10))
                .build();

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        boolean result = marketStateService.isWithinPriceBand(stockId, BigDecimal.valueOf(105));

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenPriceOutsideBand() {
        UUID stockId = UUID.randomUUID();

        Stock stock = Stock.builder()
                .id(stockId)
                .lastTradedPrice(BigDecimal.valueOf(100))
                .priceBandPercent(BigDecimal.valueOf(10))
                .build();

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        boolean result = marketStateService.isWithinPriceBand(stockId, BigDecimal.valueOf(120));

        assertThat(result).isFalse();
    }

    @Test
    void shouldThrowWhenStockNotFoundAndNoOrdersExist() {
        UUID stockId = UUID.randomUUID();

        OrderBook orderBook = mock(OrderBook.class);

        mockWithLock(stockId, orderBook);
        when(orderBook.getBuyOrders()).thenReturn(buyQueue());
        when(orderBook.getSellOrders()).thenReturn(sellQueue());
        when(stockRepository.findById(stockId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> marketStateService.calculateIndicativePrice(stockId));
    }

    private OrderBookEntry buyEntry(BigDecimal price) {
        return new OrderBookEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                OrderSide.BUY,
                price,
                10,
                Instant.now()
        );
    }

    private OrderBookEntry sellEntry(BigDecimal price) {
        return new OrderBookEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                OrderSide.SELL,
                price,
                10,
                Instant.now()
        );
    }

    private PriorityQueue<OrderBookEntry> buyQueue() {
        return new PriorityQueue<>((a, b) -> b.price().compareTo(a.price()));
    }

    private PriorityQueue<OrderBookEntry> sellQueue() {
        return new PriorityQueue<>(Comparator.comparing(OrderBookEntry::price));
    }

    private void mockWithLock(UUID stockId, OrderBook orderBook) {
        when(orderBookManager.withLock(eq(stockId), ArgumentMatchers.<Function<OrderBook, ?>>any()))
                .thenAnswer(invocation -> {
                    Function<OrderBook, ?> fn = invocation.getArgument(1);
                    return fn.apply(orderBook);
                });
    }
}