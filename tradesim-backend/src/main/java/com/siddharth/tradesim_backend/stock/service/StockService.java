package com.siddharth.tradesim_backend.stock.service;

import com.siddharth.tradesim_backend.company.enums.CompanyStatus;
import com.siddharth.tradesim_backend.company.model.Company;
import com.siddharth.tradesim_backend.company.repository.CompanyRepository;
import com.siddharth.tradesim_backend.company.CompanyException;
import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.exchange.ExchangeException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;
    private final OrderLifecycleService orderLifecycleService;
    private final MarketStateService marketStateService;
    private final ExchangeRepository exchangeRepository;
    private final CompanyRepository companyRepository;

    private record StockCap(UUID stockId, BigDecimal marketCap) {}

    @Transactional(readOnly = true)
    public List<StockResponse> fetchStocks() {
        List<Stock> allStocks = stockRepository.findAll();
        List<Exchange> allExchanges = exchangeRepository.findAll();
        Map<UUID, Exchange> exchangeMap = allExchanges.stream().collect(Collectors.toMap(Exchange::getId, e -> e));

        record StockData(Stock stock, BigDecimal price, BigDecimal marketCap) {}

        List<StockData> stockDataList = allStocks.stream().map(stock -> {
            BigDecimal price = marketStateService.calculateIndicativePrice(stock.getId());
            BigDecimal marketCap = BigDecimal.ZERO;
            if (price != null && stock.getTotalIssuedShares() != null) {
                marketCap = price.multiply(BigDecimal.valueOf(stock.getTotalIssuedShares()));
            }
            return new StockData(stock, price, marketCap);
        }).toList();

        Map<UUID, MarketCapCategory> globalCategoryMap = new HashMap<>();

        Map<UUID, List<StockData>> groupedByExchange = stockDataList.stream()
                .filter(sd -> sd.marketCap().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.groupingBy(sd -> sd.stock().getExchangeId()));

        for (Map.Entry<UUID, List<StockData>> entry : groupedByExchange.entrySet()) {
            Exchange exchange = exchangeMap.get(entry.getKey());
            if (exchange == null) continue;

            List<StockCap> sortedCaps = entry.getValue().stream()
                    .map(sd -> new StockCap(sd.stock().getId(), sd.marketCap()))
                    .sorted((a, b) -> b.marketCap().compareTo(a.marketCap()))
                    .toList();

            globalCategoryMap.putAll(calculateCategoriesForExchange(exchange, sortedCaps));
        }

        return stockDataList.stream().map(sd -> new StockResponse(
                sd.stock().getId(),
                sd.stock().getSymbol(),
                sd.stock().getCompanyName(),
                sd.price(),
                sd.stock().getSector(),
                sd.stock().getStatus(),
                sd.stock().getDayVolume() != null ? sd.stock().getDayVolume() : 0L,
                sd.marketCap(),
                globalCategoryMap.getOrDefault(sd.stock().getId(), MarketCapCategory.UNKNOWN)
        )).toList();
    }

    @Transactional(readOnly = true)
    public boolean existsBySymbol(String symbol) {
        return stockRepository.existsBySymbol(symbol);
    }

    @Transactional
    public StockResponse addStock(CreateStockRequest request) {
        Stock saved = createStock(
                request.symbol(),
                request.companyId(),
                request.exchangeId(),
                request.initialPrice(),
                request.sector(),
                request.priceBandPercent(),
                StockStatus.ACTIVE
        );

        return toResponse(saved);
    }

    @Transactional
    public StockResponse createStockFromListingApproval(UUID companyId, UUID exchangeId, String symbol, BigDecimal referencePrice, Sector sector, BigDecimal priceBandPercent) {
        Stock saved = createStock(
                symbol,
                companyId,
                exchangeId,
                referencePrice,
                sector,
                priceBandPercent,
                StockStatus.HALTED
        );

        return toResponse(saved);
    }

    @Transactional
    public StockResponse activateStockFromIssuanceApproval(UUID stockId, int totalIssuedShares, int tradableFloatShares) {
        return activateStockFromPrimaryMarketAllocation(stockId, totalIssuedShares, tradableFloatShares, "issuance approval");
    }

    @Transactional
    public StockResponse activateStockFromIpoAllotment(UUID stockId, int totalIssuedShares, int tradableFloatShares) {
        return activateStockFromPrimaryMarketAllocation(stockId, totalIssuedShares, tradableFloatShares, "IPO allotment");
    }

    @Transactional
    public StockResponse changeStockStatus(UUID stockId, StockStatus status) {
        Stock stock = stockRepository.findById(stockId).orElseThrow(() -> StockException.notFound("Stock not found"));
        if (stock.getStatus() == status) throw StockException.conflict("Stock already in this status");
        if (stock.getStatus() == StockStatus.DELISTED)
            throw StockException.conflict("Cannot change stock status of DELISTED stock");
        if (status == StockStatus.ACTIVE && stock.getStatus() == StockStatus.HALTED && (stock.getTotalIssuedShares() == null || stock.getTradableFloatShares() == null)) {
            throw StockException.conflict("Cannot activate stock before initial share allocation is completed");
        }
        try {
            if (status == StockStatus.DELISTED) {
                List<Order> openOrders = orderRepository.findByStockIdAndStatusIn(stockId, List.of(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED));

                for (Order order : openOrders) {
                    orderLifecycleService.cancelOrder(order);
                }
            }
            stock.setStatus(status);
            Stock saved = stockRepository.save(stock);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw StockException.badRequest("Invalid status data");
        }
    }

    private StockResponse activateStockFromPrimaryMarketAllocation(UUID stockId, int totalIssuedShares, int tradableFloatShares, String activationSource) {
        if (tradableFloatShares > totalIssuedShares) {
            throw StockException.badRequest("Tradable float shares cannot exceed total issued shares");
        }

        Stock stock = stockRepository.findById(stockId).orElseThrow(() -> StockException.notFound("Stock not found"));

        if (stock.getStatus() != StockStatus.HALTED) {
            throw StockException.conflict("Only HALTED stocks can be activated through " + activationSource);
        }

        if (stock.getTotalIssuedShares() != null || stock.getTradableFloatShares() != null) {
            throw StockException.conflict("Initial share allocation has already been applied to this stock");
        }

        stock.setTotalIssuedShares(totalIssuedShares);
        stock.setTradableFloatShares(tradableFloatShares);
        stock.setStatus(StockStatus.ACTIVE);

        Stock saved = stockRepository.save(stock);
        return toResponse(saved);
    }

    private Stock createStock(String symbol, UUID companyId, UUID exchangeId, BigDecimal initialPrice, Sector sector, BigDecimal priceBandPercent, StockStatus status) {
        if (stockRepository.existsBySymbol(symbol)) {
            throw StockException.conflict("Stock with symbol " + symbol + " already exists");
        }

        Company company = companyRepository.findById(companyId).orElseThrow(() -> CompanyException.notFound("Company not found"));
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw CompanyException.conflict("Company is not active");
        }

        exchangeRepository.findById(exchangeId).orElseThrow(() -> ExchangeException.notFound("Exchange not found"));

        try {
            return stockRepository.save(Stock.builder()
                    .symbol(symbol)
                    .companyName(company.getName())
                    .companyId(company.getId())
                    .exchangeId(exchangeId)
                    .lastTradedPrice(initialPrice)
                    .totalVolume(0L)
                    .sector(sector)
                    .status(status)
                    .priceBandPercent(priceBandPercent != null ? priceBandPercent : BigDecimal.TEN)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw StockException.badRequest("Invalid stock data");
        }
    }

    private StockResponse toResponse(Stock stock) {
        BigDecimal currentPrice = marketStateService.calculateIndicativePrice(stock.getId());
        BigDecimal marketCap = BigDecimal.ZERO;

        if (currentPrice != null && stock.getTotalIssuedShares() != null) {
            marketCap = currentPrice.multiply(BigDecimal.valueOf(stock.getTotalIssuedShares()));
        }

        MarketCapCategory category = resolveCategory(stock, marketCap);

        return new StockResponse(
                stock.getId(),
                stock.getSymbol(),
                stock.getCompanyName(),
                currentPrice,
                stock.getSector(),
                stock.getStatus(),
                stock.getDayVolume() != null ? stock.getDayVolume() : 0L,
                marketCap,
                category
        );
    }

    private MarketCapCategory resolveCategory(Stock targetStock, BigDecimal targetMarketCap) {
        if (targetMarketCap.compareTo(BigDecimal.ZERO) == 0) {
            return MarketCapCategory.UNKNOWN;
        }

        Exchange exchange = exchangeRepository.findById(targetStock.getExchangeId()).orElseThrow(() -> ExchangeException.notFound("Exchange not found"));
        List<Stock> exchangeStocks = stockRepository.findByExchangeId(targetStock.getExchangeId());

        List<StockCap> sortedCaps = exchangeStocks.stream()
                .map(stock -> {
                    if (stock.getId().equals(targetStock.getId())) {
                        return new StockCap(stock.getId(), targetMarketCap);
                    }
                    BigDecimal price = marketStateService.calculateIndicativePrice(stock.getId());
                    BigDecimal mc = (price != null && stock.getTotalIssuedShares() != null)
                            ? price.multiply(BigDecimal.valueOf(stock.getTotalIssuedShares()))
                            : BigDecimal.ZERO;
                    return new StockCap(stock.getId(), mc);
                })
                .filter(sc -> sc.marketCap().compareTo(BigDecimal.ZERO) > 0)
                .sorted((a, b) -> b.marketCap().compareTo(a.marketCap()))
                .toList();

        Map<UUID, MarketCapCategory> categoryMap = calculateCategoriesForExchange(exchange, sortedCaps);
        return categoryMap.getOrDefault(targetStock.getId(), MarketCapCategory.UNKNOWN);
    }

    private Map<UUID, MarketCapCategory> calculateCategoriesForExchange(Exchange exchange, List<StockCap> sortedCaps) {
        Map<UUID, MarketCapCategory> categoryMap = new HashMap<>();
        if (sortedCaps.isEmpty()) {
            return categoryMap;
        }

        boolean isIndia = "INR".equalsIgnoreCase(exchange.getCurrency()) || "IN".equalsIgnoreCase(exchange.getCountryCode());
        if (isIndia) {
            int rank = 1;
            for (StockCap sc : sortedCaps) {
                if (rank <= 100) categoryMap.put(sc.stockId(), MarketCapCategory.LARGE);
                else if (rank <= 250) categoryMap.put(sc.stockId(), MarketCapCategory.MID);
                else categoryMap.put(sc.stockId(), MarketCapCategory.SMALL);
                rank++;
            }
        } else {
            BigDecimal totalCap = sortedCaps.stream()
                    .map(StockCap::marketCap)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal cumulative = BigDecimal.ZERO;
            BigDecimal largeThreshold = totalCap.multiply(BigDecimal.valueOf(0.70));
            BigDecimal midThreshold = totalCap.multiply(BigDecimal.valueOf(0.90));

            for (StockCap sc : sortedCaps) {
                cumulative = cumulative.add(sc.marketCap());
                if (cumulative.compareTo(largeThreshold) <= 0) {
                    categoryMap.put(sc.stockId(), MarketCapCategory.LARGE);
                } else if (cumulative.compareTo(midThreshold) <= 0) {
                    categoryMap.put(sc.stockId(), MarketCapCategory.MID);
                } else {
                    categoryMap.put(sc.stockId(), MarketCapCategory.SMALL);
                }
            }
        }

        return categoryMap;
    }
}