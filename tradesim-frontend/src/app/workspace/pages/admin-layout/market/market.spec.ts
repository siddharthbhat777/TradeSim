import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { Market } from './market';
import { ExchangeService } from '../../../../services/exchange/exchange-service';
import { StockService } from '../../../../services/stock/stock-service';
import { MarketIndexService } from '../../../../services/market-index/market-index-service';
import { ForexService } from '../../../../services/forex/forex-service';

describe('Market', () => {
  let component: Market;
  let fixture: ComponentFixture<Market>;

  beforeAll(() => {
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
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Market],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ExchangeService,
          useValue: {
            getExchanges: vi.fn().mockReturnValue(of([])),
            getMarketClock: vi.fn().mockReturnValue(of({}))
          }
        },
        {
          provide: StockService,
          useValue: { getStocks: vi.fn().mockReturnValue(of([])) }
        },
        {
          provide: MarketIndexService,
          useValue: {
            getAllIndices: vi.fn().mockReturnValue(of([])),
            getConstituents: vi.fn().mockReturnValue(of([]))
          }
        },
        {
          provide: ForexService,
          useValue: { getSupportedCurrencies: vi.fn().mockReturnValue(of([])) }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Market);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});