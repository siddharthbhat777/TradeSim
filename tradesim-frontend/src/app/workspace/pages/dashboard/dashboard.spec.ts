import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal, WritableSignal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { Dashboard } from './dashboard';
import { PortfolioService } from '../../../services/portfolio/portfolio-service';
import { WalletService } from '../../../services/wallet/wallet-service';
import { TradingAccountService } from '../../../services/trading-account/trading-account-service';
import { OrderService } from '../../../services/order/order-service';

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;

  let mockPortfolioSignal: WritableSignal<any>;
  let mockWalletSignal: WritableSignal<any>;
  let mockTradingAccountSignal: WritableSignal<any>;
  let mockOrderSignal: WritableSignal<any>;

  let mockPortfolioService: any;
  let mockWalletService: any;
  let mockTradingAccountService: any;
  let mockOrderService: any;

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

    mockPortfolioSignal = signal(null);
    mockWalletSignal = signal(null);
    mockTradingAccountSignal = signal(null);
    mockOrderSignal = signal([]);

    mockPortfolioService = {
      portfolio: mockPortfolioSignal,
      loadPortfolio: vi.fn()
    };

    mockWalletService = {
      wallet: mockWalletSignal,
      loadWallet: vi.fn()
    };

    mockTradingAccountService = {
      tradingAccount: mockTradingAccountSignal,
      loadTradingAccount: vi.fn()
    };

    mockOrderService = {
      orders: mockOrderSignal,
      loadOrders: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideRouter([]),
        { provide: PortfolioService, useValue: mockPortfolioService },
        { provide: WalletService, useValue: mockWalletService },
        { provide: TradingAccountService, useValue: mockTradingAccountService },
        { provide: OrderService, useValue: mockOrderService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize services on init', () => {
    expect(mockPortfolioService.loadPortfolio).toHaveBeenCalled();
    expect(mockWalletService.loadWallet).toHaveBeenCalled();
    expect(mockTradingAccountService.loadTradingAccount).toHaveBeenCalled();
    expect(mockOrderService.loadOrders).toHaveBeenCalled();
  });

  it('should compute summary metrics correctly', () => {
    mockTradingAccountSignal.set({ baseCurrency: 'USD' });
    mockPortfolioSignal.set({
      equity: 15000,
      totalInvested: 10000,
      totalUnrealizedPnl: 500
    });
    mockWalletSignal.set({
      buckets: [{ currency: 'USD', availableBalance: 5000 }]
    });

    fixture.detectChanges();

    expect(component.baseCurrency()).toBe('USD');
    expect(component.equity()).toBe(15000);
    expect(component.totalInvested()).toBe(10000);
    expect(component.unrealizedPnl()).toBe(500);
    expect(component.buyingPower()).toBe(5000);
  });

  it('should compute allocation data with cash and group excess holdings', () => {
    mockTradingAccountSignal.set({ baseCurrency: 'USD' });
    mockPortfolioSignal.set({
      totalCashValue: 6000,
      marginLoan: 1000,
      holdings: [
        { stockId: '1', symbol: 'AAPL', currentValue: 4000 },
        { stockId: '2', symbol: 'TSLA', currentValue: 3000 },
        { stockId: '3', symbol: 'MSFT', currentValue: 2000 },
        { stockId: '4', symbol: 'AMZN', currentValue: 1000 },
        { stockId: '5', symbol: 'GOOGL', currentValue: 500 },
        { stockId: '6', symbol: 'META', currentValue: 300 }
      ]
    });

    fixture.detectChanges();

    const allocation = component.allocationData();

    expect(allocation.length).toBe(6);

    const cashSlice = allocation.find(s => s.id === 'cash-slice');
    expect(cashSlice).toBeTruthy();
    expect(cashSlice?.value).toBe(5000);

    const othersSlice = allocation.find(s => s.id === 'others-slice');
    expect(othersSlice).toBeTruthy();
    expect(othersSlice?.value).toBe(800);
    expect(othersSlice?.label).toBe('Other Assets');
  });

  it('should compute orders data correctly', () => {
    const mockOrders = [{ orderId: '123', symbol: 'TSLA' }];
    mockOrderSignal.set(mockOrders);
    fixture.detectChanges();

    expect(component.ordersData()).toEqual(mockOrders);
  });
});