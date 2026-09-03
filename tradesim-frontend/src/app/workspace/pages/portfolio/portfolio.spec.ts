import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal, WritableSignal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { of } from 'rxjs';
import { Portfolio } from './portfolio';
import { PortfolioService } from '../../../services/portfolio/portfolio-service';
import { RiskService } from '../../../services/risk/risk-service';
import { TradingAccountService } from '../../../services/trading-account/trading-account-service';

describe('Portfolio', () => {
  let component: Portfolio;
  let fixture: ComponentFixture<Portfolio>;

  let mockPortfolioSignal: WritableSignal<any>;
  let mockTradingAccountSignal: WritableSignal<any>;

  let mockPortfolioService: any;
  let mockRiskService: any;
  let mockTradingAccountService: any;

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

    Object.defineProperty(window, 'ResizeObserver', {
      writable: true,
      value: vi.fn().mockImplementation(() => ({
        observe: vi.fn(),
        unobserve: vi.fn(),
        disconnect: vi.fn(),
      })),
    });

    mockPortfolioSignal = signal(null);
    mockTradingAccountSignal = signal(null);

    mockPortfolioService = {
      portfolio: mockPortfolioSignal,
      loadPortfolio: vi.fn(),
      getPortfolioHistory: vi.fn().mockReturnValue(of([
        { snapshotDate: '2026-09-01', equity: 1412000 },
        { snapshotDate: '2026-09-02', equity: 1415000 }
      ]))
    };

    mockRiskService = {
      getMyRisk: vi.fn().mockReturnValue(of({
        equity: 15000,
        marginUsed: 5000,
        maintenanceMargin: 2000,
        unrealizedPnl: 500,
        marginRatio: 1.5,
        riskLevel: 'SAFE',
        isUnderLiquidation: false
      }))
    };

    mockTradingAccountService = {
      tradingAccount: mockTradingAccountSignal,
      loadTradingAccount: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [Portfolio],
      providers: [
        provideRouter([]),
        { provide: PortfolioService, useValue: mockPortfolioService },
        { provide: RiskService, useValue: mockRiskService },
        { provide: TradingAccountService, useValue: mockTradingAccountService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Portfolio);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize services on init', () => {
    expect(mockTradingAccountService.loadTradingAccount).toHaveBeenCalled();
    expect(mockPortfolioService.loadPortfolio).toHaveBeenCalled();
    expect(mockPortfolioService.getPortfolioHistory).toHaveBeenCalled();
    expect(mockRiskService.getMyRisk).toHaveBeenCalled();
  });

  it('should compute baseCurrency correctly', () => {
    mockTradingAccountSignal.set({ baseCurrency: 'USD' });
    fixture.detectChanges();
    expect(component.baseCurrency()).toBe('USD');
  });

  it('should compute exposureData correctly including cash balance', () => {
    mockPortfolioSignal.set({
      totalCashValue: 5000,
      holdings: [
        { stockId: '1', symbol: 'AAPL', currentValue: 4000 },
        { stockId: '2', symbol: 'TSLA', currentValue: 1000 }
      ]
    });

    fixture.detectChanges();

    const exposure = component.exposureData();

    expect(exposure.length).toBe(3);

    expect(exposure[0].id).toBe('cash');
    expect(exposure[0].value).toBe(5000);
    expect(exposure[0].color).toBe('var(--success)');

    expect(exposure[1].id).toBe('1');
    expect(exposure[1].label).toBe('AAPL');
    expect(exposure[1].value).toBe(4000);

    expect(exposure[2].id).toBe('2');
    expect(exposure[2].label).toBe('TSLA');
    expect(exposure[2].value).toBe(1000);
  });
});