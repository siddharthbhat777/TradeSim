import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Market } from './market';
import { ExchangeService } from '../../../services/exchange/exchange-service';
import { MarketIndexService } from '../../../services/market-index/market-index-service';
import { StockService } from '../../../services/stock/stock-service';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('Market', () => {
  let component: Market;
  let fixture: ComponentFixture<Market>;
  let exchangeServiceSpy: any;
  let marketIndexServiceSpy: any;
  let stockServiceSpy: any;

  beforeEach(async () => {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation(query => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    });

    exchangeServiceSpy = {
      getExchanges: vi.fn().mockReturnValue(of([
        { id: 'ex-1', name: 'NASDAQ', code: 'NDX', country: 'US', timezone: 'America/New_York', currency: 'USD', marketOpenTime: '09:30', marketCloseTime: '16:00', status: 'ACTIVE' }
      ]))
    };

    marketIndexServiceSpy = {
      getIndicesByExchange: vi.fn().mockReturnValue(of([
        { id: 'idx-1', name: 'TradeSim Benchmark 50', symbol: 'TS50', exchangeId: 'ex-1', baseValue: 1000 }
      ]))
    };

    stockServiceSpy = {
      getStocks: vi.fn().mockReturnValue(of([
        { id: 's-1', symbol: 'AAPL', companyName: 'Apple Inc', currentPrice: 150, sector: 'TECHNOLOGY', status: 'ACTIVE', dayVolume: 100, marketCap: 10000000000, marketCapCategory: 'LARGE' },
        { id: 's-2', symbol: 'TSLA', companyName: 'Tesla Motors', currentPrice: 250, sector: 'AUTOMOTIVE', status: 'ACTIVE', dayVolume: 200, marketCap: 15000000000, marketCapCategory: 'LARGE' }
      ]))
    };

    await TestBed.configureTestingModule({
      imports: [Market],
      providers: [
        { provide: ExchangeService, useValue: exchangeServiceSpy },
        { provide: MarketIndexService, useValue: marketIndexServiceSpy },
        { provide: StockService, useValue: stockServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Market);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load exchanges and stocks on init', () => {
    expect(exchangeServiceSpy.getExchanges).toHaveBeenCalled();
    expect(stockServiceSpy.getStocks).toHaveBeenCalled();
    expect(component.exchanges().length).toBe(1);
    expect(component.rawStocks().length).toBe(2);
  });

  it('should dynamically calculate maxStockPrice based on highest currentPrice', () => {
    expect(component.maxStockPrice()).toBe(300);
  });

  it('should filter stocks based on search query', () => {
    component.searchQuery.set('Tesla');

    const filtered = component.filteredAndSortedStocks();

    expect(filtered.length).toBe(1);
    expect(filtered[0].symbol).toBe('TSLA');
  });

  it('should open filter drawer and synchronize drafts with applied state', () => {
    component.appliedPriceRange.set([0, 150]);

    component.openFilterDrawer();

    expect(component.isFilterDrawerOpen()).toBe(true);
    expect(component.draftPriceRange()).toEqual([0, 150]);
  });

  it('should detect unsaved filters accurately', () => {
    expect(component.hasUnsavedFilters()).toBe(false);

    component.draftPriceRange.set([0, 100]);

    expect(component.hasUnsavedFilters()).toBe(true);
  });

  it('should apply draft filters to the live table and close the drawer', () => {
    component.openFilterDrawer();
    component.draftPriceRange.set([0, 200]);

    component.applyFilters();

    expect(component.appliedPriceRange()).toEqual([0, 200]);
    expect(component.isFilterDrawerOpen()).toBe(false);

    const filtered = component.filteredAndSortedStocks();
    expect(filtered.length).toBe(1);
    expect(filtered[0].symbol).toBe('AAPL');
  });

  it('should reset draft filters without affecting applied filters', () => {
    component.openFilterDrawer();
    component.draftSectors.set(['TECHNOLOGY']);

    component.resetDraftFilters();

    expect(component.draftSectors().length).toBe(0);
    expect(component.appliedSectors().length).toBe(0);
  });
});