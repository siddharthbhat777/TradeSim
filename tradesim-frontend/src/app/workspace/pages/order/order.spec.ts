import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Order } from './order';
import { OrderService } from '../../../services/order/order-service';
import { TradingAccountService } from '../../../services/trading-account/trading-account-service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { DialogService } from '../../../shared/components/dialog/dialog.service';
import { signal } from '@angular/core';

describe('Order', () => {
  let component: Order;
  let fixture: ComponentFixture<Order>;

  beforeEach(async () => {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: () => ({
        matches: false,
        addListener: () => { },
        removeListener: () => { }
      })
    });

    const mockOrderService = {
      orders: signal([]),
      loadOrders: () => { },
      cancelOrder: () => { }
    };

    const mockTradingAccountService = {
      tradingAccount: signal({ baseCurrency: 'USD' }),
      loadTradingAccount: () => { }
    };

    const mockToastService = {
      success: () => { }
    };

    const mockDialogService = {
      open: () => { },
      close: () => { }
    };

    await TestBed.configureTestingModule({
      imports: [Order],
      providers: [
        { provide: OrderService, useValue: mockOrderService },
        { provide: TradingAccountService, useValue: mockTradingAccountService },
        { provide: ToastService, useValue: mockToastService },
        { provide: DialogService, useValue: mockDialogService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Order);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});