import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Order } from './order';
import { OrderService } from '../../../services/order/order-service';
import { TradingAccountService } from '../../../services/trading-account/trading-account-service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { DialogService } from '../../../shared/components/dialog/dialog.service';

describe('Order', () => {
  let component: Order;
  let fixture: ComponentFixture<Order>;

  const mockOrderService = {
    orders: signal([]),
    loadOrders: () => { },
    cancelOrder: () => ({ subscribe: () => { } })
  };

  const mockTradingAccountService = {
    tradingAccount: signal(null),
    loadTradingAccount: () => { }
  };

  const mockToastService = {
    success: () => { },
    danger: () => { }
  };

  const mockDialogService = {
    open: () => { }
  };

  beforeEach(async () => {
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
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});