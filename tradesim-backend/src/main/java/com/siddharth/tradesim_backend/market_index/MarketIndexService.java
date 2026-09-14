package com.siddharth.tradesim_backend.market_index;

import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.exchange.ExchangeService;
import com.siddharth.tradesim_backend.market_index.model.MarketIndex;
import com.siddharth.tradesim_backend.market_index.model.MarketIndexConstituent;
import com.siddharth.tradesim_backend.market_index.model.dto.AddConstituentRequest;
import com.siddharth.tradesim_backend.market_index.model.dto.CreateMarketIndexRequest;
import com.siddharth.tradesim_backend.market_index.model.dto.MarketIndexConstituentResponse;
import com.siddharth.tradesim_backend.market_index.model.dto.MarketIndexResponse;
import com.siddharth.tradesim_backend.market_index.repository.MarketIndexConstituentRepository;
import com.siddharth.tradesim_backend.market_index.repository.MarketIndexRepository;
import com.siddharth.tradesim_backend.stock.StockException;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarketIndexService {
    private final MarketIndexRepository marketIndexRepository;
    private final MarketIndexConstituentRepository marketIndexConstituentRepository;
    private final ExchangeRepository exchangeRepository;
    private final ExchangeService exchangeService;
    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public List<MarketIndexResponse> fetchAllIndices() {
        return marketIndexRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MarketIndexResponse> fetchIndicesByExchange(UUID exchangeId) {
        return marketIndexRepository.findByExchangeId(exchangeId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MarketIndexConstituentResponse> fetchConstituents(UUID indexId) {
        if (!marketIndexRepository.existsById(indexId)) {
            throw MarketIndexException.notFound("Index not found");
        }

        return marketIndexConstituentRepository.findByIndexId(indexId).stream()
                .map(constituent -> {
                    Stock stock = stockRepository.findById(constituent.getStockId()).orElse(null);
                    return new MarketIndexConstituentResponse(
                            constituent.getStockId(),
                            stock != null ? stock.getSymbol() : "UNKNOWN",
                            stock != null ? stock.getCompanyName() : "UNKNOWN"
                    );
                })
                .toList();
    }

    @Transactional
    public MarketIndexResponse createIndex(CreateMarketIndexRequest request) {
        if (marketIndexRepository.existsBySymbol(request.symbol())) {
            throw MarketIndexException.conflict("Index with this symbol already exists");
        }

        if (!exchangeRepository.existsById(request.exchangeId())) {
            throw MarketIndexException.badRequest("Invalid exchange ID");
        }

        try {
            MarketIndex index = MarketIndex.builder()
                    .name(request.name())
                    .symbol(request.symbol())
                    .exchangeId(request.exchangeId())
                    .baseValue(request.baseValue())
                    .build();

            return toResponse(marketIndexRepository.save(index));
        } catch (DataIntegrityViolationException e) {
            throw MarketIndexException.badRequest("Invalid index data");
        }
    }

    @Transactional
    public void addConstituent(UUID indexId, AddConstituentRequest request) {
        MarketIndex index = marketIndexRepository.findById(indexId).orElseThrow(() -> MarketIndexException.notFound("Index not found"));
        Stock stock = stockRepository.findById(request.stockId()).orElseThrow(() -> StockException.notFound("Stock not found"));

        if (!stock.getExchangeId().equals(index.getExchangeId())) {
            throw MarketIndexException.conflict("Stock exchange must match index exchange");
        }

        if (marketIndexConstituentRepository.existsByIndexIdAndStockId(indexId, stock.getId())) {
            throw MarketIndexException.conflict("Stock is already a constituent of this index");
        }

        MarketIndexConstituent constituent = MarketIndexConstituent.builder()
                .indexId(indexId)
                .stockId(stock.getId())
                .build();

        marketIndexConstituentRepository.save(constituent);
    }

    @Transactional
    public void removeConstituent(UUID indexId, UUID stockId) {
        List<MarketIndexConstituent> constituents = marketIndexConstituentRepository.findByIndexId(indexId);
        MarketIndexConstituent target = constituents.stream()
                .filter(constituent -> constituent.getStockId().equals(stockId))
                .findFirst()
                .orElseThrow(() -> MarketIndexException.notFound("Constituent not found"));

        marketIndexConstituentRepository.delete(target);
    }

    @Transactional
    public MarketIndexResponse initializeIndex(UUID indexId) {
        MarketIndex index = marketIndexRepository.findById(indexId).orElseThrow(() -> MarketIndexException.notFound("Index not found"));

        BigDecimal currentMarketCap = calculateCurrentMarketCap(indexId);

        if (currentMarketCap.compareTo(BigDecimal.ZERO) == 0) {
            throw MarketIndexException.conflict("Cannot initialize index with zero market cap (ensure stocks have float shares and prices)");
        }

        LocalDate today = exchangeService.currentExchangeDate(index.getExchangeId());

        index.setBaseMarketCap(currentMarketCap);
        index.setCurrentValue(index.getBaseValue());
        index.setPreviousClose(index.getBaseValue());
        index.setDayOpen(index.getBaseValue());
        index.setDayHigh(index.getBaseValue());
        index.setDayLow(index.getBaseValue());
        index.setLastTradingDate(today);

        return toResponse(marketIndexRepository.save(index));
    }

    @Transactional
    public void updateIndicesForStock(UUID stockId) {
        List<MarketIndexConstituent> constituents = marketIndexConstituentRepository.findByStockId(stockId);
        for (MarketIndexConstituent constituent : constituents) {
            recalculateIndex(constituent.getIndexId());
        }
    }

    private void recalculateIndex(UUID indexId) {
        MarketIndex index = marketIndexRepository.findById(indexId).orElseThrow(() -> MarketIndexException.notFound("Index not found"));

        if (index.getBaseMarketCap() == null || index.getBaseMarketCap().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        BigDecimal currentMarketCap = calculateCurrentMarketCap(indexId);
        BigDecimal currentValue = currentMarketCap
                .divide(index.getBaseMarketCap(), 8, RoundingMode.HALF_UP)
                .multiply(index.getBaseValue())
                .setScale(4, RoundingMode.HALF_UP);

        LocalDate today = exchangeService.currentExchangeDate(index.getExchangeId());

        if (index.getLastTradingDate() == null || !index.getLastTradingDate().equals(today)) {
            index.setPreviousClose(index.getCurrentValue() != null ? index.getCurrentValue() : index.getBaseValue());
            index.setDayOpen(currentValue);
            index.setDayHigh(currentValue);
            index.setDayLow(currentValue);
            index.setLastTradingDate(today);
        } else {
            if (index.getDayHigh() == null || currentValue.compareTo(index.getDayHigh()) > 0) {
                index.setDayHigh(currentValue);
            }
            if (index.getDayLow() == null || currentValue.compareTo(index.getDayLow()) < 0) {
                index.setDayLow(currentValue);
            }
        }

        index.setCurrentValue(currentValue);
        marketIndexRepository.save(index);
    }

    private BigDecimal calculateCurrentMarketCap(UUID indexId) {
        List<MarketIndexConstituent> constituents = marketIndexConstituentRepository.findByIndexId(indexId);
        BigDecimal totalMarketCap = BigDecimal.ZERO;

        for (MarketIndexConstituent constituent : constituents) {
            Stock stock = stockRepository.findById(constituent.getStockId()).orElse(null);
            if (stock != null && stock.getTradableFloatShares() != null && stock.getLastTradedPrice() != null) {
                BigDecimal stockMarketCap = stock.getLastTradedPrice().multiply(BigDecimal.valueOf(stock.getTradableFloatShares()));
                totalMarketCap = totalMarketCap.add(stockMarketCap);
            }
        }

        return totalMarketCap;
    }

    private MarketIndexResponse toResponse(MarketIndex index) {
        BigDecimal change = BigDecimal.ZERO;
        BigDecimal changePercent = BigDecimal.ZERO;

        if (index.getCurrentValue() != null && index.getPreviousClose() != null && index.getPreviousClose().compareTo(BigDecimal.ZERO) > 0) {
            change = index.getCurrentValue().subtract(index.getPreviousClose()).setScale(4, RoundingMode.HALF_UP);
            changePercent = change.multiply(BigDecimal.valueOf(100)).divide(index.getPreviousClose(), 4, RoundingMode.HALF_UP);
        }

        return new MarketIndexResponse(
                index.getId(),
                index.getName(),
                index.getSymbol(),
                index.getExchangeId(),
                index.getBaseValue(),
                index.getCurrentValue(),
                change,
                changePercent,
                index.getDayOpen(),
                index.getDayHigh(),
                index.getDayLow(),
                index.getPreviousClose()
        );
    }
}