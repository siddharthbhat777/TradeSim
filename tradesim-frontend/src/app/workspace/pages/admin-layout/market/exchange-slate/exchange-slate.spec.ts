import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { ExchangeSlate } from './exchange-slate';
import { ExchangeService } from '../../../../../services/exchange/exchange-service';
import { ForexService } from '../../../../../services/forex/forex-service';
import { ToastService } from '../../../../../shared/components/toast/toast.service';

describe('ExchangeSlate', () => {
  let component: ExchangeSlate;
  let fixture: ComponentFixture<ExchangeSlate>;

  const mockExchangeService = {
    createExchange: vi.fn().mockReturnValue(of({}))
  };

  const mockForexService = {
    countries: signal([]),
    loadCountries: vi.fn(),
    getCurrencyForCountry: vi.fn().mockReturnValue(of('{"currency":"INR","timezones":["Asia/Kolkata"]}'))
  };

  const mockToast = {
    success: vi.fn(),
    danger: vi.fn()
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExchangeSlate],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ExchangeService, useValue: mockExchangeService },
        { provide: ForexService, useValue: mockForexService },
        { provide: ToastService, useValue: mockToast }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ExchangeSlate);
    component = fixture.componentInstance;

    // Provide required inputs
    fixture.componentRef.setInput('exchanges', []);
    fixture.componentRef.setInput('selectedExchangeId', null);
    fixture.componentRef.setInput('clocks', new Map());
    fixture.componentRef.setInput('stocksCount', 0);
    fixture.componentRef.setInput('indicesCount', 0);

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});