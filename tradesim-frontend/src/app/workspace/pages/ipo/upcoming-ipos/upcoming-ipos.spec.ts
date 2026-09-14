import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { UpcomingIpos } from './upcoming-ipos';
import { IpoService } from '../../../../services/ipo/ipo-service';
import { StockService } from '../../../../services/stock/stock-service';
import { TradingAccountService } from '../../../../services/trading-account/trading-account-service';

describe('UpcomingIpos', () => {
  let component: UpcomingIpos;
  let fixture: ComponentFixture<UpcomingIpos>;

  beforeEach(async () => {
    const mockIpoService = {
      getUpcomingIpos: vi.fn().mockReturnValue(of([]))
    };

    const mockStockService = {
      getStocks: vi.fn().mockReturnValue(of([]))
    };

    const mockTradingAccountService = {
      tradingAccount: signal({ baseCurrency: 'INR' }),
      loadTradingAccount: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [UpcomingIpos],
      providers: [
        { provide: IpoService, useValue: mockIpoService },
        { provide: StockService, useValue: mockStockService },
        { provide: TradingAccountService, useValue: mockTradingAccountService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UpcomingIpos);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});