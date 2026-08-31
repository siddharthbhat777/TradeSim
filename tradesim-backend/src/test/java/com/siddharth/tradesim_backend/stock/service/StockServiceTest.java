package com.siddharth.tradesim_backend.stock.service;

import com.siddharth.tradesim_backend.company.repository.CompanyRepository;
import com.siddharth.tradesim_backend.company.enums.CompanyStatus;
import com.siddharth.tradesim_backend.company.model.Company;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.exchange.model.Exchange;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.order.service.OrderLifecycleService;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.enums.MarketCapCategory;
import com.siddharth.tradesim_backend.stock.enums.Sector;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.StockException;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.stock.model.dto.CreateStockRequest;
import com.siddharth.tradesim_backend.stock.model.dto.StockResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderLifecycleService orderLifecycleService;

    @Mock
    private ExchangeRepository exchangeRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private MarketStateService marketStateService;

    @InjectMocks
    private StockService stockService;

    @Test
    void shouldChangeStockStatusWhenValid() {
        UUID stockId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();

        Stock stock = Stock.builder()
                .id(stockId)
                .exchangeId(exchangeId)
                .status(StockStatus.ACTIVE)
                .build();

        Exchange exchange = Exchange.builder().id(exchangeId).currency("USD").build();

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(marketStateService.calculateIndicativePrice(any())).thenReturn(BigDecimal.valueOf(150.00));
        when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));
        when(stockRepository.findByExchangeId(exchangeId)).thenReturn(List.of(stock));

        StockResponse response = stockService.changeStockStatus(stockId, StockStatus.HALTED);

        assertThat(response.status()).isEqualTo(StockStatus.HALTED);
    }

    @Test
    void shouldThrowExceptionWhenStockIsAlreadyDelisted() {
        UUID stockId = UUID.randomUUID();

        Stock stock = Stock.builder()
                .id(stockId)
                .status(StockStatus.DELISTED)
                .build();

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        assertThrows(StockException.class, () -> stockService.changeStockStatus(stockId, StockStatus.ACTIVE));

        verify(stockRepository, never()).save(any());
    }

    @Test
    void shouldCancelAllOpenAndPartiallyFilledOrdersWhenStockIsDelisted() {
        UUID stockId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();

        Stock stock = Stock.builder()
                .id(stockId)
                .exchangeId(exchangeId)
                .status(StockStatus.ACTIVE)
                .build();

        Exchange exchange = Exchange.builder().id(exchangeId).currency("USD").build();

        Order openOrder = Order.builder()
                .status(OrderStatus.OPEN)
                .build();

        Order partialOrder = Order.builder()
                .status(OrderStatus.PARTIALLY_FILLED)
                .build();

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(orderRepository.findByStockIdAndStatusIn(eq(stockId), eq(List.of(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED)))).thenReturn(List.of(openOrder, partialOrder));
        when(stockRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(marketStateService.calculateIndicativePrice(any())).thenReturn(BigDecimal.valueOf(150.00));
        when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));
        when(stockRepository.findByExchangeId(exchangeId)).thenReturn(List.of(stock));

        stockService.changeStockStatus(stockId, StockStatus.DELISTED);

        verify(orderLifecycleService).cancelOrder(openOrder);
        verify(orderLifecycleService).cancelOrder(partialOrder);
        verify(stockRepository).save(stock);
    }

    @Test
    void shouldNotCancelAnythingWhenNoOpenOrdersExist() {
        UUID stockId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();

        Stock stock = Stock.builder()
                .id(stockId)
                .exchangeId(exchangeId)
                .status(StockStatus.ACTIVE)
                .build();

        Exchange exchange = Exchange.builder().id(exchangeId).currency("USD").build();

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(orderRepository.findByStockIdAndStatusIn(eq(stockId), eq(List.of(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED)))).thenReturn(List.of());
        when(stockRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(marketStateService.calculateIndicativePrice(any())).thenReturn(BigDecimal.valueOf(150.00));
        when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));
        when(stockRepository.findByExchangeId(exchangeId)).thenReturn(List.of(stock));

        stockService.changeStockStatus(stockId, StockStatus.DELISTED);

        verify(orderLifecycleService, never()).cancelOrder(any());
    }

    @Test
    void shouldAddStockWhenCompanyAndExchangeAreValid() {
        UUID companyId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();

        CreateStockRequest request = new CreateStockRequest(
                "AAPL",
                companyId,
                exchangeId,
                BigDecimal.valueOf(150.25),
                Sector.TECHNOLOGY,
                BigDecimal.TEN
        );

        Company company = Company.builder()
                .id(companyId)
                .name("Apple Inc")
                .status(CompanyStatus.ACTIVE)
                .build();

        Exchange exchange = Exchange.builder().id(exchangeId).currency("USD").build();

        when(stockRepository.existsBySymbol(request.symbol())).thenReturn(false);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));
        when(stockRepository.save(any(Stock.class))).thenAnswer(i -> {
            Stock stock = i.getArgument(0);
            stock.setId(UUID.randomUUID());
            return stock;
        });
        when(marketStateService.calculateIndicativePrice(any())).thenReturn(BigDecimal.valueOf(150.25));
        when(stockRepository.findByExchangeId(exchangeId)).thenReturn(List.of());

        StockResponse response = stockService.addStock(request);

        assertThat(response.symbol()).isEqualTo("AAPL");
        assertThat(response.companyName()).isEqualTo("Apple Inc");
        verify(stockRepository).save(any(Stock.class));
    }

    @Test
    void shouldRejectInactiveCompanyWhenAddingStock() {
        UUID companyId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();

        CreateStockRequest request = new CreateStockRequest(
                "AAPL",
                companyId,
                exchangeId,
                BigDecimal.valueOf(150.25),
                Sector.TECHNOLOGY,
                BigDecimal.TEN
        );

        Company company = Company.builder()
                .id(companyId)
                .name("Apple Inc")
                .status(CompanyStatus.INACTIVE)
                .build();

        when(stockRepository.existsBySymbol(request.symbol())).thenReturn(false);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        BusinessException exception = assertThrows(BusinessException.class, () -> stockService.addStock(request));

        assertThat(exception.getMessage()).isEqualTo("Company is not active");
        verify(stockRepository, never()).save(any());
    }

    @Test
    void shouldCreateApprovedListingStockInHaltedStatus() {
        UUID companyId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();

        Company company = Company.builder()
                .id(companyId)
                .name("Apple Inc")
                .status(CompanyStatus.ACTIVE)
                .build();

        Exchange exchange = Exchange.builder().id(exchangeId).currency("USD").build();

        when(stockRepository.existsBySymbol("AAPL")).thenReturn(false);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));
        when(stockRepository.save(any(Stock.class))).thenAnswer(invocation -> {
            Stock stock = invocation.getArgument(0);
            stock.setId(UUID.randomUUID());
            return stock;
        });
        when(marketStateService.calculateIndicativePrice(any())).thenReturn(BigDecimal.valueOf(150.25));
        when(stockRepository.findByExchangeId(exchangeId)).thenReturn(List.of());

        StockResponse response = stockService.createStockFromListingApproval(
                companyId,
                exchangeId,
                "AAPL",
                BigDecimal.valueOf(150.25),
                Sector.TECHNOLOGY,
                BigDecimal.TEN
        );

        assertThat(response.status()).isEqualTo(StockStatus.HALTED);
        verify(stockRepository).save(any(Stock.class));
    }

    @Test
    void shouldActivateStockFromApprovedIpoAllotmentAndStoreShareMetadata() {
        UUID stockId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();

        Stock stock = Stock.builder()
                .id(stockId)
                .symbol("TS_MOTORS")
                .exchangeId(exchangeId)
                .companyName("TradeSim Motors Limited")
                .lastTradedPrice(BigDecimal.valueOf(250.50))
                .sector(Sector.INDUSTRIALS)
                .status(StockStatus.HALTED)
                .build();

        Exchange exchange = Exchange.builder().id(exchangeId).currency("INR").build();

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any(Stock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(marketStateService.calculateIndicativePrice(any())).thenReturn(BigDecimal.valueOf(250.50));
        when(exchangeRepository.findById(exchangeId)).thenReturn(Optional.of(exchange));

        when(stockRepository.findByExchangeId(exchangeId)).thenReturn(List.of(stock));

        StockResponse response = stockService.activateStockFromIpoAllotment(stockId, 500, 500);

        assertThat(response.status()).isEqualTo(StockStatus.ACTIVE);
        assertThat(stock.getTotalIssuedShares()).isEqualTo(500);
        assertThat(stock.getTradableFloatShares()).isEqualTo(500);
        assertThat(response.marketCapCategory()).isEqualTo(MarketCapCategory.LARGE);
        verify(stockRepository).save(stock);
    }

    @Test
    void shouldNotAllowManualActivationBeforeInitialShareAllocation() {
        UUID stockId = UUID.randomUUID();

        Stock stock = Stock.builder()
                .id(stockId)
                .status(StockStatus.HALTED)
                .build();

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        StockException exception = assertThrows(StockException.class, () -> stockService.changeStockStatus(stockId, StockStatus.ACTIVE));

        assertThat(exception.getMessage()).isEqualTo("Cannot activate stock before initial share allocation is completed");
        verify(stockRepository, never()).save(any());
    }

    @Test
    void shouldFetchStocksAndCalculateIndicativePrice() {
        UUID stockId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();

        Stock stock = Stock.builder()
                .id(stockId)
                .symbol("AAPL")
                .exchangeId(exchangeId)
                .totalIssuedShares(1000)
                .lastTradedPrice(BigDecimal.valueOf(150))
                .build();

        Exchange exchange = Exchange.builder().id(exchangeId).currency("USD").build();

        when(stockRepository.findAll()).thenReturn(List.of(stock));
        when(exchangeRepository.findAll()).thenReturn(List.of(exchange));
        when(marketStateService.calculateIndicativePrice(stockId)).thenReturn(BigDecimal.valueOf(155.50));

        List<StockResponse> responses = stockService.fetchStocks();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().currentPrice()).isEqualByComparingTo(BigDecimal.valueOf(155.50));
        verify(marketStateService).calculateIndicativePrice(stockId);
    }
}