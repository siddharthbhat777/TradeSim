import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrderTicket } from './order-ticket';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { Stock } from '../../../../../models/stock';

describe('OrderTicket', () => {
  let component: OrderTicket;
  let fixture: ComponentFixture<OrderTicket>;

  const mockStock = { id: 's-1', symbol: 'AAPL', currentPrice: 150 } as Stock;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderTicket]
    }).compileComponents();

    fixture = TestBed.createComponent(OrderTicket);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('stock', mockStock);
    fixture.componentRef.setInput('wallet', { multiCurrencyStatus: 'APPROVED', buckets: [{ currency: 'USD', availableBalance: 100 }] });
    fixture.componentRef.setInput('baseCurrency', 'INR');
    fixture.componentRef.setInput('supportedCurrencies', ['INR', 'USD']);

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should calculate financials correctly', () => {
    component.orderQuantity.set(2);
    component.orderType.set('LIMIT');
    component.limitPrice.set(100);

    const fins = component.financials();
    expect(fins.subtotalInStockCurrency).toBe(200);
    expect(fins.hasFunds).toBe(false);
  });

  it('should emit review event', () => {
    const emitSpy = vi.spyOn(component.reviewOrder, 'emit');
    component.onReviewClick();
    expect(emitSpy).toHaveBeenCalledWith({
      orderSide: 'BUY',
      orderType: 'MARKET',
      timeInForce: 'DAY',
      orderQuantity: 1,
      limitPrice: 150,
      fundingCurrency: 'INR'
    });
  });
});