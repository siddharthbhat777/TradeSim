package com.siddharth.tradesim_backend.stock.service;

import com.siddharth.tradesim_backend.company.repository.CompanyRepository;
import com.siddharth.tradesim_backend.company.enums.CompanyStatus;
import com.siddharth.tradesim_backend.company.model.Company;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.order.service.OrderLifecycleService;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.enums.Sector;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.exceptions.CreateStockException;
import com.siddharth.tradesim_backend.stock.exceptions.StockStatusException;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.stock.model.dto.CreateStockRequest;
import com.siddharth.tradesim_backend.stock.model.dto.StockResponse;
import com.siddharth.tradesim_backend.user.exceptions.StatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;
    private final OrderLifecycleService orderLifecycleService;
    private final MarketStateService marketStateService;
    private final ExchangeRepository exchangeRepository;
    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public List<StockResponse> fetchStocks() {
        return stockRepository.findAll()
                .stream()
                .map(stock -> new StockResponse(
                        stock.getId(),
                        stock.getSymbol(),
                        stock.getCompanyName(),
                        marketStateService.calculateIndicativePrice(stock.getId()),
                        stock.getSector(),
                        stock.getStatus()
                ))
                .toList();
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
        if (tradableFloatShares > totalIssuedShares) {
            throw new BusinessException("Tradable float shares cannot exceed total issued shares");
        }

        Stock stock = stockRepository.findById(stockId).orElseThrow(() -> new BusinessException("Stock not found"));

        if (stock.getStatus() != StockStatus.HALTED) {
            throw new BusinessException("Only HALTED stocks can be activated through issuance");
        }

        if (stock.getTotalIssuedShares() != null || stock.getTradableFloatShares() != null) {
            throw new BusinessException("Initial issuance has already been applied to this stock");
        }

        stock.setTotalIssuedShares(totalIssuedShares);
        stock.setTradableFloatShares(tradableFloatShares);
        stock.setStatus(StockStatus.ACTIVE);

        Stock saved = stockRepository.save(stock);
        return toResponse(saved);
    }

    @Transactional
    public StockResponse changeStockStatus(UUID stockId, StockStatus status) {
        Stock stock = stockRepository.findById(stockId).orElseThrow(() -> new BusinessException("Stock not found"));
        if (stock.getStatus() == status) throw new StockStatusException("Stock already in this status");
        if (stock.getStatus() == StockStatus.DELISTED)
            throw new StockStatusException("Cannot change stock status of DELISTED stock");
        if (status == StockStatus.ACTIVE && stock.getStatus() == StockStatus.HALTED && (stock.getTotalIssuedShares() == null || stock.getTradableFloatShares() == null)) {
            throw new StockStatusException("Cannot activate stock before initial issuance is approved");
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
            return new StockResponse(
                    saved.getId(),
                    saved.getSymbol(),
                    saved.getCompanyName(),
                    saved.getLastTradedPrice(),
                    saved.getSector(),
                    saved.getStatus()
            );
        } catch (DataIntegrityViolationException e) {
            throw new StatusException("Invalid status data");
        } catch (Exception e) {
            throw new StatusException("Unable to change status of stock");
        }
    }

    private Stock createStock(String symbol, UUID companyId, UUID exchangeId, BigDecimal initialPrice, Sector sector, BigDecimal priceBandPercent, StockStatus status) {
        if (stockRepository.existsBySymbol(symbol)) {
            throw new CreateStockException("Stock with symbol " + symbol + " already exists");
        }

        Company company = companyRepository.findById(companyId).orElseThrow(() -> new BusinessException("Company not found"));
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new BusinessException("Company is not active");
        }

        exchangeRepository.findById(exchangeId).orElseThrow(() -> new BusinessException("Exchange not found"));

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
            throw new CreateStockException("Invalid stock data");
        }
    }

    private StockResponse toResponse(Stock stock) {
        return new StockResponse(
                stock.getId(),
                stock.getSymbol(),
                stock.getCompanyName(),
                stock.getLastTradedPrice(),
                stock.getSector(),
                stock.getStatus()
        );
    }
}