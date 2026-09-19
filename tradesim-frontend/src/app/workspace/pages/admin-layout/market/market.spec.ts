import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { Market } from './market';
import { ExchangeService } from '../../../../services/exchange/exchange-service';
import { StockService } from '../../../../services/stock/stock-service';
import { MarketIndexService } from '../../../../services/market-index/market-index-service';
import { ForexService } from '../../../../services/forex/forex-service';
import { DialogService } from '../../../../shared/components/dialog/dialog.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { Exchange, ExchangeMarketClock } from '../../../../models/exchange';
import { Stock } from '../../../../models/stock';
import { MarketIndex } from '../../../../models/market-index';

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

  const mockExchange: Exchange = {
    id: 'ex-1',
    name: 'National Stock Exchange',
    code: 'NSE',
    countryCode: 'IN',
    timezone: 'Asia/Kolkata',
    currency: 'INR',
    marketOpenTime: '09:15:00',
    marketCloseTime: '15:30:00',
    status: 'ACTIVE'
  };

  const mockClock: ExchangeMarketClock = {
    exchangeId: 'ex-1',
    exchangeCode: 'NSE',
    exchangeName: 'National Stock Exchange',
    timezone: 'Asia/Kolkata',
    localDate: '2026-09-19',
    localTime: '10:00:00',
    localDayOfWeek: 'SATURDAY',
    marketOpenTime: '09:15:00',
    marketCloseTime: '15:30:00',
    tradingDay: false,
    marketOpenNow: false,
    currentInstant: '2026-09-19T04:30:00Z',
    todayMarketOpenAt: '2026-09-19T03:45:00Z',
    todayMarketCloseAt: '2026-09-19T10:00:00Z'
  };

  const mockStock: Stock = {
    id: 'stk-1',
    exchangeId: 'ex-1',
    symbol: 'INFY',
    companyName: 'Infosys Limited',
    sector: 'TECHNOLOGY',
    currentPrice: 1850,
    dayVolume: 500000,
    marketCap: 750000000000,
    marketCapCategory: 'LARGE',
    currency: 'INR',
    status: 'ACTIVE'
  };

  const mockIndex: MarketIndex = {
    id: 'idx-1',
    name: 'NIFTY 50',
    symbol: 'NIFTY',
    exchangeId: 'ex-1',
    baseValue: 1000,
    currentValue: 24500,
    change: 120,
    changePercent: 0.5,
    dayOpen: 24400,
    dayHigh: 24520,
    dayLow: 24390,
    previousClose: 24380
  };

  let exchangeServiceMock: any;
  let stockServiceMock: any;
  let marketIndexServiceMock: any;
  let forexServiceMock: any;
  let dialogServiceMock: any;
  let toastServiceMock: any;

  beforeEach(async () => {
    exchangeServiceMock = {
      getExchanges: vi.fn().mockReturnValue(of([mockExchange])),
      getMarketClock: vi.fn().mockReturnValue(of(mockClock))
    };
    stockServiceMock = {
      getStocks: vi.fn().mockReturnValue(of([mockStock])),
      changeStockStatus: vi.fn().mockReturnValue(of({ ...mockStock, status: 'HALTED' }))
    };
    marketIndexServiceMock = {
      getAllIndices: vi.fn().mockReturnValue(of([mockIndex])),
      getConstituents: vi.fn().mockReturnValue(of([]))
    };
    forexServiceMock = {
      getSupportedCurrencies: vi.fn().mockReturnValue(of(['USD', 'INR', 'EUR'])),
      getExchangeRate: vi.fn().mockReturnValue(of(83.5))
    };
    dialogServiceMock = {
      open: vi.fn().mockImplementation((config: any) => {
        if (config.onPrimary) config.onPrimary();
      })
    };
    toastServiceMock = {
      success: vi.fn(),
      danger: vi.fn(),
      warning: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [Market],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ExchangeService, useValue: exchangeServiceMock },
        { provide: StockService, useValue: stockServiceMock },
        { provide: MarketIndexService, useValue: marketIndexServiceMock },
        { provide: ForexService, useValue: forexServiceMock },
        { provide: DialogService, useValue: dialogServiceMock },
        { provide: ToastService, useValue: toastServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Market);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    component.ngOnDestroy();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load initial data and select the first exchange', () => {
    expect(component.exchanges().length).toBe(1);
    expect(component.selectedExchangeId()).toBe('ex-1');
    expect(component.currentExchange()?.code).toBe('NSE');
    expect(component.exchangeStocks().length).toBe(1);
    expect(component.exchangeIndices().length).toBe(1);
  });

  it('should filter stocks based on selected exchange', () => {
    expect(component.filteredAndSortedStocks().length).toBe(1);
    expect(component.filteredAndSortedStocks()[0].symbol).toBe('INFY');

    component.onExchangeSelect('non-existent-exchange');
    expect(component.exchangeStocks().length).toBe(0);
    expect(component.filteredAndSortedStocks().length).toBe(0);
  });

  it('should trigger status change and update local stocks signal', () => {
    component.changeStockStatus(mockStock, 'HALTED');

    expect(dialogServiceMock.open).toHaveBeenCalled();
    expect(stockServiceMock.changeStockStatus).toHaveBeenCalledWith('stk-1', 'HALTED');
    expect(toastServiceMock.success).toHaveBeenCalledWith('Status updated successfully');

    const updated = component.stocks().find(s => s.id === 'stk-1');
    expect(updated?.status).toBe('HALTED');
  });

  it('should calculate exchange rate for forex tab', () => {
    component.sourceCurrency.setValue('USD');
    component.targetCurrency.setValue('INR');
    component.amountToConvert.setValue(100);

    component.calculateRate();

    expect(forexServiceMock.getExchangeRate).toHaveBeenCalledWith('USD', 'INR');
    expect(component.convertedRate()).toBe(8350);
  });

  it('should swap currencies properly in forex tab', () => {
    component.sourceCurrency.setValue('USD');
    component.targetCurrency.setValue('EUR');

    component.swapCurrencies();

    expect(component.sourceCurrency.value).toBe('EUR');
    expect(component.targetCurrency.value).toBe('USD');
    expect(component.convertedRate()).toBeNull();
  });
});