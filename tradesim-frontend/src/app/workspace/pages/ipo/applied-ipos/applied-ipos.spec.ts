import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { AppliedIpos } from './applied-ipos';
import { IpoService } from '../../../../services/ipo/ipo-service';
import { StockService } from '../../../../services/stock/stock-service';
import { TradingAccountService } from '../../../../services/trading-account/trading-account-service';
import { ToastService } from '../../../../shared/components/toast/toast.service';

describe('AppliedIpos', () => {
  let component: AppliedIpos;
  let fixture: ComponentFixture<AppliedIpos>;

  const mockIpoService = {
    getMySubscriptions: () => of([])
  };

  const mockStockService = {
    getStocks: () => of([])
  };

  const mockTradingAccountService = {
    tradingAccount: signal(null),
    loadTradingAccount: () => { }
  };

  const mockToastService = {
    success: () => { },
    danger: () => { }
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppliedIpos],
      providers: [
        { provide: IpoService, useValue: mockIpoService },
        { provide: StockService, useValue: mockStockService },
        { provide: TradingAccountService, useValue: mockTradingAccountService },
        { provide: ToastService, useValue: mockToastService }
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