import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { OngoingIpos } from './ongoing-ipos';
import { IpoService } from '../../../../services/ipo/ipo-service';
import { StockService } from '../../../../services/stock/stock-service';
import { TradingAccountService } from '../../../../services/trading-account/trading-account-service';
import { DialogService } from '../../../../shared/components/dialog/dialog.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';

describe('OngoingIpos', () => {
  let component: OngoingIpos;
  let fixture: ComponentFixture<OngoingIpos>;

  beforeEach(async () => {
    const mockIpoService = {
      getOpenIpos: vi.fn().mockReturnValue(of([])),
      subscribeToIpo: vi.fn().mockReturnValue(of({}))
    };

    const mockStockService = {
      getStocks: vi.fn().mockReturnValue(of([]))
    };

    const mockTradingAccountService = {
      tradingAccount: signal({ baseCurrency: 'INR' }),
      loadTradingAccount: vi.fn()
    };

    const mockDialogService = {
      open: vi.fn(),
      close: vi.fn()
    };

    const mockToastService = {
      success: vi.fn(),
      danger: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [OngoingIpos],
      providers: [
        { provide: IpoService, useValue: mockIpoService },
        { provide: StockService, useValue: mockStockService },
        { provide: TradingAccountService, useValue: mockTradingAccountService },
        { provide: DialogService, useValue: mockDialogService },
        { provide: ToastService, useValue: mockToastService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OngoingIpos);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});