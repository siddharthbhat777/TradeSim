import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FundManager } from './fund-manager';
import { describe, it, expect, vi, beforeEach } from 'vitest';

describe('FundManager', () => {
  let component: FundManager;
  let fixture: ComponentFixture<FundManager>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FundManager]
    }).compileComponents();

    fixture = TestBed.createComponent(FundManager);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('baseCurrency', 'INR');
    fixture.componentRef.setInput('targetCurrency', 'USD');
    fixture.componentRef.setInput('baseBalance', 1000);
    fixture.componentRef.setInput('targetBalance', 50);
    fixture.componentRef.setInput('liveConversionRate', 0.012);
    fixture.componentRef.setInput('isFetchingRate', false);
    fixture.componentRef.setInput('isProcessingFund', false);

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should compute estimated conversion amounts accurately', () => {
    component.convertAmount.set(100);
    expect(component.estimatedConvertedAmount()).toBe(1.2);
    expect(component.estimatedConversionFxFee()).toBe(0.012);
  });

  it('should emit processDeposit event', () => {
    const emitSpy = vi.spyOn(component.processDeposit, 'emit');
    component.depositAmount.set(500);
    component.onDeposit();
    expect(emitSpy).toHaveBeenCalledWith(500);
  });
});