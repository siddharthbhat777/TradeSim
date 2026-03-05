package com.siddharth.tradesim_backend.stock.service;

import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.orderbook.OrderBook;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookEntry;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookManager;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import static org.assertj.core.api.Assertions.assertThat;
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

        OrderBook orderBook = mock(OrderBook.class);
        PriorityQueue<OrderBookEntry> buyOrders = new PriorityQueue<>((a, b) -> b.price().compareTo(a.price()));
        PriorityQueue<OrderBookEntry> sellOrders = new PriorityQueue<>(Comparator.comparing(OrderBookEntry::price));

        buyOrders.add(entry(BigDecimal.valueOf(100)));
        sellOrders.add(entry(BigDecimal.valueOf(110)));

        when(orderBookManager.getOrderBook(stockId)).thenReturn(orderBook);
        when(orderBook.getBuyOrders()).thenReturn(buyOrders);
        when(orderBook.getSellOrders()).thenReturn(sellOrders);

        BigDecimal price = marketStateService.calculateIndicativePrice(stockId);

        assertThat(price).isEqualByComparingTo("105.0000");
    }

    @Test
    void shouldReturnBidPriceWhenOnlyBidExists() {
        UUID stockId = UUID.randomUUID();

        OrderBook orderBook = mock(OrderBook.class);

        PriorityQueue<OrderBookEntry> buyOrders = new PriorityQueue<>((a, b) -> b.price().compareTo(a.price()));
        PriorityQueue<OrderBookEntry> sellOrders = new PriorityQueue<>();

        buyOrders.add(entry(BigDecimal.valueOf(120)));

        when(orderBookManager.getOrderBook(stockId)).thenReturn(orderBook);
        when(orderBook.getBuyOrders()).thenReturn(buyOrders);
        when(orderBook.getSellOrders()).thenReturn(sellOrders);

        BigDecimal price = marketStateService.calculateIndicativePrice(stockId);

        assertThat(price).isEqualByComparingTo("120");
    }

    @Test
    void shouldReturnAskPriceWhenOnlyAskExists() {
        UUID stockId = UUID.randomUUID();

        OrderBook orderBook = mock(OrderBook.class);

        PriorityQueue<OrderBookEntry> buyOrders = new PriorityQueue<>();
        PriorityQueue<OrderBookEntry> sellOrders = new PriorityQueue<>(Comparator.comparing(OrderBookEntry::price));

        sellOrders.add(entry(BigDecimal.valueOf(130)));

        when(orderBookManager.getOrderBook(stockId)).thenReturn(orderBook);
        when(orderBook.getBuyOrders()).thenReturn(buyOrders);
        when(orderBook.getSellOrders()).thenReturn(sellOrders);

        BigDecimal price = marketStateService.calculateIndicativePrice(stockId);

        assertThat(price).isEqualByComparingTo("130");
    }

    @Test
    void shouldReturnLastTradedPriceWhenNoOrdersExist() {
        UUID stockId = UUID.randomUUID();

        OrderBook orderBook = mock(OrderBook.class);

        PriorityQueue<OrderBookEntry> buyOrders = new PriorityQueue<>();
        PriorityQueue<OrderBookEntry> sellOrders = new PriorityQueue<>();

        Stock stock = Stock.builder()
                .id(stockId)
                .lastTradedPrice(BigDecimal.valueOf(95))
                .build();

        when(orderBookManager.getOrderBook(stockId)).thenReturn(orderBook);
        when(orderBook.getBuyOrders()).thenReturn(buyOrders);
        when(orderBook.getSellOrders()).thenReturn(sellOrders);
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
        assertThat(stock.getDayVolume()).isEqualTo(10);
        assertThat(stock.getLastTradedPrice()).isEqualByComparingTo("100");
        assertThat(stock.getTotalVolume()).isEqualTo(10);

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
        assertThat(stock.getDayVolume()).isEqualTo(25);
        assertThat(stock.getTotalVolume()).isEqualTo(55);

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

    private OrderBookEntry entry(BigDecimal price) {
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
}