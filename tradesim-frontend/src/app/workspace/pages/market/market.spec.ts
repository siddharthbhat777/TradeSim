import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Market } from './market';
import { ExchangeService } from '../../../services/exchange/exchange-service';
import { MarketIndexService } from '../../../services/market-index/market-index-service';
import { StockService } from '../../../services/stock/stock-service';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

class ResizeObserverMock {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
}

describe('Market', () => {
  let component: Market;
  let fixture: ComponentFixture<Market>;
  let exchangeServiceSpy: any;
  let marketIndexServiceSpy: any;
  let stockServiceSpy: any;
  let routerSpy: any;
  let routeSpy: any;

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
    vi.stubGlobal('ResizeObserver', ResizeObserverMock);

    exchangeServiceSpy = {
      getExchanges: vi.fn().mockReturnValue(of([
        { id: 'ex-1', name: 'NASDAQ', code: 'NDX', countryCode: 'US', timezone: 'America/New_York', currency: 'USD', marketOpenTime: '09:30', marketCloseTime: '16:00', status: 'ACTIVE' }
      ])),
      getMarketClock: vi.fn().mockReturnValue(of({
        currentInstant: '2026-08-31T10:00:00Z', timezone: 'America/New_York', marketOpenNow: true
      }))
    };

    marketIndexServiceSpy = {
      getIndicesByExchange: vi.fn().mockReturnValue(of([
        { id: 'idx-1', name: 'TradeSim Benchmark 50', symbol: 'TS50', exchangeId: 'ex-1', baseValue: 1000, currentValue: 1000, change: 0, changePercent: 0, dayOpen: 1000, dayHigh: 1000, dayLow: 1000, previousClose: 1000 }
      ]))
    };

    stockServiceSpy = {
      getStocks: vi.fn().mockReturnValue(of([
        { id: 's-1', symbol: 'AAPL', companyName: 'Apple Inc', currentPrice: 150, sector: 'TECHNOLOGY', status: 'ACTIVE', dayVolume: 100, marketCap: 10000000000, marketCapCategory: 'LARGE', currency: 'USD', exchangeId: 'ex-1' },
        { id: 's-2', symbol: 'TSLA', companyName: 'Tesla Motors', currentPrice: 250, sector: 'AUTOMOTIVE', status: 'ACTIVE', dayVolume: 200, marketCap: 15000000000, marketCapCategory: 'LARGE', currency: 'USD', exchangeId: 'ex-1' }
      ]))
    };

    routerSpy = {
      navigate: vi.fn()
    };

    routeSpy = {
      snapshot: { queryParamMap: { get: vi.fn().mockReturnValue(null) } },
      queryParamMap: of({ get: vi.fn().mockReturnValue(null) })
    };

    await TestBed.configureTestingModule({
      imports: [Market],
      providers: [
        { provide: ExchangeService, useValue: exchangeServiceSpy },
        { provide: MarketIndexService, useValue: marketIndexServiceSpy },
        { provide: StockService, useValue: stockServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: routeSpy }
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
});