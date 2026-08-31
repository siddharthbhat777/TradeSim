import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { RecentOrders } from './recent-orders';
import { OrderHistoryResponse } from '../../../../models/order';

describe('RecentOrders', () => {
  let component: RecentOrders;
  let fixture: ComponentFixture<RecentOrders>;

  const mockOrders: OrderHistoryResponse[] = [
    {
      orderId: '1',
      stockId: 's1',
      symbol: 'AAPL',
      side: 'BUY',
      orderType: 'MARKET',
      quantity: 10,
      filledQuantity: 10,
      limitPrice: null,
      status: 'FILLED',
      createdAt: '2023-01-01T10:00:00Z'
    },
    {
      orderId: '2',
      stockId: 's2',
      symbol: 'TSLA',
      side: 'SELL',
      orderType: 'LIMIT',
      quantity: 5,
      filledQuantity: 2,
      limitPrice: 200,
      status: 'PARTIALLY_FILLED',
      createdAt: '2023-01-02T10:00:00Z'
    }
  ];

  beforeEach(async () => {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation(query => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    });

    await TestBed.configureTestingModule({
      imports: [RecentOrders],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(RecentOrders);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('orders', mockOrders);
    fixture.componentRef.setInput('currency', 'USD');

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should correctly format order statuses to sentence case without underscores', () => {
    const formatStatus = (component as any).formatStatus.bind(component);

    expect(formatStatus('OPEN')).toBe('Open');
    expect(formatStatus('PARTIALLY_FILLED')).toBe('Partially filled');
    expect(formatStatus('CANCELLED')).toBe('Cancelled');
    expect(formatStatus('FILLED')).toBe('Filled');
  });
});