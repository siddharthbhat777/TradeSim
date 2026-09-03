import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StockDetails } from './stock-details';
import { OrderService } from '../../../../services/order/order-service';
import { WalletService } from '../../../../services/wallet/wallet-service';
import { TradingAccountService } from '../../../../services/trading-account/trading-account-service';
import { ForexService } from '../../../../services/forex/forex-service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';
import { Stock } from '../../../../models/stock';
import { OrderTicketPayload } from './order-ticket/order-ticket';

class ResizeObserverMock {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
}

describe('StockDetails', () => {
  let component: StockDetails;
  let fixture: ComponentFixture<StockDetails>;
  let orderServiceSpy: any;
  let walletServiceSpy: any;
  let tradingAccountServiceSpy: any;
  let forexServiceSpy: any;
  let toastServiceSpy: any;

  const mockStock: Stock = {
    id: 's-1',
    symbol: 'AAPL',
    companyName: 'Apple Inc',
    currentPrice: 150,
    sector: 'TECHNOLOGY',
    status: 'ACTIVE',
    dayVolume: 1000,
    marketCap: 1000000,
    marketCapCategory: 'LARGE'
  };

  const dummyPayload: OrderTicketPayload = {
    orderSide: 'BUY',
    orderType: 'MARKET',
    timeInForce: 'DAY',
    orderQuantity: 5,
    limitPrice: null,
    fundingCurrency: 'USD'
  };

  beforeEach(async () => {
    vi.stubGlobal('matchMedia', vi.fn().mockImplementation(query => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })));

    vi.stubGlobal('ResizeObserver', ResizeObserverMock);

    orderServiceSpy = {
      createOrder: vi.fn().mockReturnValue(of({})),
      estimateOrder: vi.fn().mockReturnValue(of({
        subtotalInFundingCurrency: 150,
        safetyBufferInFundingCurrency: 0,
        fxFee: 1.5,
        finalTotal: 151.5,
        hasFunds: true,
        fundingCurrency: 'USD'
      }))
    };

    walletServiceSpy = {
      wallet: signal({
        multiCurrencyStatus: 'APPROVED',
        buckets: [{ currency: 'USD', availableBalance: 1000 }]
      }),
      loadWallet: vi.fn(),
      deposit: vi.fn().mockReturnValue(of({})),
      convert: vi.fn().mockReturnValue(of({}))
    };

    tradingAccountServiceSpy = {
      tradingAccount: signal({
        baseCurrency: 'USD'
      }),
      loadTradingAccount: vi.fn()
    };

    forexServiceSpy = {
      getSupportedCurrencies: vi.fn().mockReturnValue(of(['USD', 'EUR'])),
      getExchangeRate: vi.fn().mockReturnValue(of(1.1))
    };

    toastServiceSpy = {
      success: vi.fn(),
      danger: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [StockDetails],
      providers: [
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: WalletService, useValue: walletServiceSpy },
        { provide: TradingAccountService, useValue: tradingAccountServiceSpy },
        { provide: ForexService, useValue: forexServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StockDetails);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('stock', mockStock);

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit back event when onBack is called', () => {
    const emitSpy = vi.spyOn(component.back, 'emit');
    component.onBack();
    expect(emitSpy).toHaveBeenCalled();
  });

  it('should call OrderService and show review modal on reviewOrder', () => {
    component.onReviewOrder(dummyPayload);

    expect(orderServiceSpy.estimateOrder).toHaveBeenCalledWith({
      stockId: 's-1',
      quantity: 5,
      side: 'BUY',
      orderType: 'MARKET',
      timeInForce: 'DAY',
      limitPrice: null,
      fundingCurrency: 'USD'
    });
    expect(component.showReviewModal()).toBe(true);
    expect(component.activeOrderPayload()).toEqual(dummyPayload);
  });

  it('should place order and show success drawer on executeOrder', () => {
    component.activeOrderPayload.set(dummyPayload);
    component.onExecuteOrder();

    expect(orderServiceSpy.createOrder).toHaveBeenCalledWith({
      stockId: 's-1',
      quantity: 5,
      side: 'BUY',
      orderType: 'MARKET',
      timeInForce: 'DAY',
      limitPrice: null,
      fundingCurrency: 'USD'
    });
    expect(component.showReviewModal()).toBe(false);
    expect(component.showSuccessDrawer()).toBe(true);
    expect(walletServiceSpy.loadWallet).toHaveBeenCalled();
  });

  it('should fetch exchange rate and open deposit modal', () => {
    walletServiceSpy.wallet.set({
      buckets: [{ currency: 'USD', availableBalance: 1000 }]
    });

    component.onOpenDeposit('EUR');

    expect(forexServiceSpy.getExchangeRate).toHaveBeenCalledWith('USD', 'EUR');
    expect(component.showFundModal()).toBe(true);
    expect(component.liveConversionRate()).toBe(1.1);
  });
});