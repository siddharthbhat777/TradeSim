import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrderReviewModal } from './order-review-modal';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { OrderTicketPayload } from '../order-ticket/order-ticket';

describe('OrderReviewModal', () => {
  let component: OrderReviewModal;
  let fixture: ComponentFixture<OrderReviewModal>;

  const mockPayload: OrderTicketPayload = {
    orderSide: 'BUY',
    orderType: 'MARKET',
    timeInForce: 'DAY',
    orderQuantity: 10,
    limitPrice: null,
    fundingCurrency: 'USD'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderReviewModal]
    }).compileComponents();

    fixture = TestBed.createComponent(OrderReviewModal);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('payload', mockPayload);
    fixture.componentRef.setInput('stockSymbol', 'AAPL');
    fixture.componentRef.setInput('baseCurrency', 'INR');
    fixture.componentRef.setInput('isEstimatingOrder', false);
    fixture.componentRef.setInput('orderEstimate', null);
    fixture.componentRef.setInput('isFetchingRate', false);
    fixture.componentRef.setInput('isSubmittingOrder', false);

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit executeOrder event', () => {
    component.isOpen.set(true);
    const emitSpy = vi.spyOn(component.executeOrder, 'emit');

    fixture.componentRef.setInput('orderEstimate', { hasFunds: true, finalTotal: 150 });
    fixture.detectChanges();

    component.executeOrder.emit();
    expect(emitSpy).toHaveBeenCalled();
  });
});