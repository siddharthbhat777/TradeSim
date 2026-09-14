import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StockHeader } from './stock-header';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { Stock } from '../../../../../models/stock';

describe('StockHeader', () => {
  let component: StockHeader;
  let fixture: ComponentFixture<StockHeader>;

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
  } as Stock;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StockHeader]
    }).compileComponents();

    fixture = TestBed.createComponent(StockHeader);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('stock', mockStock);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit back event when onBack is called', () => {
    const emitSpy = vi.spyOn(component.back, 'emit');
    component.onBack();
    expect(emitSpy).toHaveBeenCalled();
  });
});