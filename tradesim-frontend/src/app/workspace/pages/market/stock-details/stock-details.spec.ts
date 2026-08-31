import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StockDetails } from './stock-details';
import { OrderService } from '../../../../services/order/order-service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('StockDetails', () => {
  let component: StockDetails;
  let fixture: ComponentFixture<StockDetails>;
  let orderServiceSpy: any;
  let toastServiceSpy: any;

  const mockStock = {
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

    orderServiceSpy = {
      createOrder: vi.fn().mockReturnValue(of({}))
    };

    toastServiceSpy = {
      success: vi.fn(),
      danger: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [StockDetails],
      providers: [
        { provide: OrderService, useValue: orderServiceSpy },
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

  it('should compute estimatedTotal correctly for MARKET orders', () => {
    component.orderQuantity.set(10);
    component.orderType.set('MARKET');
    expect(component.estimatedTotal()).toBe(1500);
  });

  it('should compute estimatedTotal correctly for LIMIT orders', () => {
    component.orderQuantity.set(10);
    component.orderType.set('LIMIT');
    component.limitPrice.set(145);
    expect(component.estimatedTotal()).toBe(1450);
  });

  it('should emit back event when onBack is called', () => {
    const emitSpy = vi.spyOn(component.back, 'emit');
    component.onBack();
    expect(emitSpy).toHaveBeenCalled();
  });

  it('should prevent placing order if quantity is 0 or less', () => {
    component.orderQuantity.set(0);
    component.placeOrder();
    expect(toastServiceSpy.danger).toHaveBeenCalledWith('Quantity must be greater than zero.');
    expect(orderServiceSpy.createOrder).not.toHaveBeenCalled();
  });

  it('should prevent placing LIMIT order if limitPrice is invalid', () => {
    component.orderType.set('LIMIT');
    component.limitPrice.set(0);
    component.placeOrder();
    expect(toastServiceSpy.danger).toHaveBeenCalledWith('Please enter a valid limit price.');
    expect(orderServiceSpy.createOrder).not.toHaveBeenCalled();
  });

  it('should call OrderService and show success toast on successful order placement', () => {
    component.orderSide.set('BUY');
    component.orderType.set('MARKET');
    component.timeInForce.set('DAY');
    component.orderQuantity.set(5);

    component.placeOrder();

    expect(orderServiceSpy.createOrder).toHaveBeenCalledWith({
      stockId: 's-1',
      quantity: 5,
      side: 'BUY',
      orderType: 'MARKET',
      timeInForce: 'DAY',
      limitPrice: null
    });
    expect(toastServiceSpy.success).toHaveBeenCalledWith('BUY order for AAPL placed successfully!');
    expect(component.isSubmittingOrder()).toBe(false);
  });

  it('should handle error when placing order fails', () => {
    orderServiceSpy.createOrder.mockReturnValue(throwError(() => new Error('Error')));
    component.isSubmittingOrder.set(true);

    component.placeOrder();

    expect(component.isSubmittingOrder()).toBe(false);
    expect(toastServiceSpy.success).not.toHaveBeenCalled();
  });
});