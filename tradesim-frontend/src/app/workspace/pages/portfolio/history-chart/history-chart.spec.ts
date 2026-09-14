import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { vi } from 'vitest';
import { HistoryChart } from './history-chart';

class ResizeObserverMock {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
}

describe('HistoryChart', () => {
  let component: HistoryChart;
  let fixture: ComponentFixture<HistoryChart>;

  beforeEach(async () => {
    vi.stubGlobal('matchMedia', vi.fn().mockImplementation(query => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })));

    vi.stubGlobal('ResizeObserver', ResizeObserverMock);

    await TestBed.configureTestingModule({
      imports: [HistoryChart]
    }).compileComponents();

    fixture = TestBed.createComponent(HistoryChart);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('data', [
      { time: '2026-09-01', value: 10000 },
      { time: '2026-09-02', value: 10500 }
    ]);
    fixture.componentRef.setInput('baseCurrency', 'USD');

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render the chart component with correct inputs', () => {
    const chartElement = fixture.debugElement.query(By.css('app-area-chart'));
    expect(chartElement).toBeTruthy();

    expect(component.baseCurrency()).toBe('USD');
    expect(component.data().length).toBe(2);
  });
});