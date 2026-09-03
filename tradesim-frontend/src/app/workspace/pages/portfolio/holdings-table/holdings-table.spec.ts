import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { vi } from 'vitest';
import { HoldingsTable } from './holdings-table';

describe('HoldingsTable', () => {
  let component: HoldingsTable;
  let fixture: ComponentFixture<HoldingsTable>;

  const mockHoldings = [
    {
      stockId: '1',
      symbol: 'AAPL',
      quantity: 10,
      averageBuyPrice: 150,
      currentPrice: 175,
      currentValue: 1750,
      unrealizedPnl: 250
    },
    {
      stockId: '2',
      symbol: 'TSLA',
      quantity: 5,
      averageBuyPrice: 200,
      currentPrice: 180,
      currentValue: 900,
      unrealizedPnl: -100
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
      imports: [HoldingsTable],
    }).compileComponents();

    fixture = TestBed.createComponent(HoldingsTable);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('holdings', []);
    fixture.componentRef.setInput('baseCurrency', 'USD');

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with correct default page size and columns', () => {
    expect(component.pageSize()).toBe(10);
    expect(component.columns.length).toBe(6);
    expect(component.columns.map(c => c.key)).toEqual([
      'symbol',
      'quantity',
      'averageBuyPrice',
      'currentPrice',
      'currentValue',
      'unrealizedPnl'
    ]);
  });

  it('should render empty state when holdings array is empty', () => {
    fixture.componentRef.setInput('holdings', []);
    fixture.detectChanges();

    const emptyState = fixture.debugElement.query(By.css('app-empty-state'));
    expect(emptyState).toBeTruthy();

    const rows = fixture.debugElement.queryAll(By.css('tbody tr'));
    expect(rows.length).toBe(0);
  });

  it('should render table rows when holdings data is provided', () => {
    fixture.componentRef.setInput('holdings', mockHoldings);
    fixture.detectChanges();

    const rows = fixture.debugElement.queryAll(By.css('tbody tr'));
    expect(rows.length).toBe(2);

    const firstRowCells = rows[0].queryAll(By.css('td'));
    expect(firstRowCells[0].nativeElement.textContent.trim()).toBe('AAPL');
  });

  it('should filter holdings when search query is entered', () => {
    fixture.componentRef.setInput('holdings', mockHoldings);
    component.searchQuery.set('TSLA');
    fixture.detectChanges();

    expect(component.filteredHoldings().length).toBe(1);
    expect(component.filteredHoldings()[0].symbol).toBe('TSLA');
  });
});