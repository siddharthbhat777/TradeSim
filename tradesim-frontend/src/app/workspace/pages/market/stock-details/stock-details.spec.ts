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

  it('should compute financials correctly for MARKET orders', () => {
    component.orderQuantity.set(10);
    component.orderType.set('MARKET');
    expect(component.financials().subtotalInStockCurrency).toBe(1500);
    expect(component.financials().hasFunds).toBe(true);
  });

  it('should compute financials correctly for LIMIT orders', () => {
    component.orderQuantity.set(10);
    component.orderType.set('LIMIT');
    component.limitPrice.set(145);
    expect(component.financials().subtotalInStockCurrency).toBe(1450);
  });

  it('should emit back event when onBack is called', () => {
    const emitSpy = vi.spyOn(component.back, 'emit');
    component.onBack();
    expect(emitSpy).toHaveBeenCalled();
  });

  it('should prevent reviewing order if quantity is 0 or less', () => {
    component.orderQuantity.set(0);
    component.reviewOrder();
    expect(toastServiceSpy.danger).toHaveBeenCalledWith('Quantity must be greater than zero.');
    expect(orderServiceSpy.estimateOrder).not.toHaveBeenCalled();
  });

  it('should prevent reviewing LIMIT order if limitPrice is invalid', () => {
    component.orderType.set('LIMIT');
    component.limitPrice.set(0);
    component.reviewOrder();
    expect(toastServiceSpy.danger).toHaveBeenCalledWith('Please enter a valid limit price.');
    expect(orderServiceSpy.estimateOrder).not.toHaveBeenCalled();
  });

  it('should call OrderService and show review modal on reviewOrder', () => {
    component.orderSide.set('BUY');
    component.orderType.set('MARKET');
    component.timeInForce.set('DAY');
    component.orderQuantity.set(5);

    component.reviewOrder();

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
  });

  it('should place order and show success drawer on executeOrder', () => {
    component.orderSide.set('BUY');
    component.orderType.set('MARKET');
    component.timeInForce.set('DAY');
    component.orderQuantity.set(5);

    component.executeOrder();

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

  it('should handle error when placing order fails', () => {
    orderServiceSpy.createOrder.mockReturnValue(throwError(() => new Error('Error')));
    component.isSubmittingOrder.set(true);

    component.executeOrder();

    expect(component.isSubmittingOrder()).toBe(false);
    expect(component.showSuccessDrawer()).toBe(false);
  });

  it('should fetch exchange rate and open deposit modal', () => {
    component.fundingMethod.set('CUSTOM');
    component.customCurrency.set('EUR');

    walletServiceSpy.wallet.set({
      buckets: [{ currency: 'USD', availableBalance: 1000 }]
    });

    component.openDeposit();

    expect(forexServiceSpy.getExchangeRate).toHaveBeenCalledWith('USD', 'EUR');
    expect(component.showFundModal()).toBe(true);
    expect(component.liveConversionRate()).toBe(1.1);
  });
});