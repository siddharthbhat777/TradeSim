import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Clock } from './clock';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ExchangeService } from '../../../../services/exchange/exchange-service';

describe('Clock', () => {
  let component: Clock;
  let fixture: ComponentFixture<Clock>;
  let exchangeServiceSpy: any;

  beforeEach(async () => {
    exchangeServiceSpy = {
      getMarketClock: vi.fn().mockReturnValue(of({
        exchangeId: 'ex-1',
        exchangeCode: 'NDX',
        exchangeName: 'NASDAQ',
        timezone: 'America/New_York',
        localDate: '2026-08-31',
        localTime: '09:30:00',
        localDayOfWeek: 'MONDAY',
        marketOpenTime: '09:30:00',
        marketCloseTime: '16:00:00',
        tradingDay: true,
        marketOpenNow: true,
        currentInstant: '2026-08-31T13:30:00Z',
        todayMarketOpenAt: '2026-08-31T13:30:00Z',
        todayMarketCloseAt: '2026-08-31T20:00:00Z'
      }))
    };

    await TestBed.configureTestingModule({
      imports: [Clock],
      providers: [
        { provide: ExchangeService, useValue: exchangeServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Clock);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should load market clock data when exchangeId is provided', async () => {
    fixture.componentRef.setInput('exchangeId', 'ex-1');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(exchangeServiceSpy.getMarketClock).toHaveBeenCalledWith('ex-1');
    expect(component.isLoaded()).toBe(true);
    expect(component.isOpen()).toBe(true);
    expect(component.timezone()).toBe('America/New_York');
    expect(component.currentTime()).toBeInstanceOf(Date);
  });

  it('should reset loaded state when exchangeId is removed', () => {
    fixture.componentRef.setInput('exchangeId', 'ex-1');
    fixture.detectChanges();

    fixture.componentRef.setInput('exchangeId', null);
    fixture.detectChanges();

    expect(component.isLoaded()).toBe(false);
  });
});