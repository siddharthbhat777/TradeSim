import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { AppliedIpos } from './applied-ipos';
import { IpoService } from '../../../../services/ipo/ipo-service';
import { StockService } from '../../../../services/stock/stock-service';
import { TradingAccountService } from '../../../../services/trading-account/trading-account-service';

describe('AppliedIpos', () => {
  let component: AppliedIpos;
  let fixture: ComponentFixture<AppliedIpos>;

  beforeEach(async () => {
    const mockIpoService = {
      getMySubscriptions: vi.fn().mockReturnValue(of([]))
    };

    const mockStockService = {
      getStocks: vi.fn().mockReturnValue(of([]))
    };

    const mockTradingAccountService = {
      tradingAccount: signal({ baseCurrency: 'INR' }),
      loadTradingAccount: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [AppliedIpos],
      providers: [
        { provide: IpoService, useValue: mockIpoService },
        { provide: StockService, useValue: mockStockService },
        { provide: TradingAccountService, useValue: mockTradingAccountService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AppliedIpos);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});