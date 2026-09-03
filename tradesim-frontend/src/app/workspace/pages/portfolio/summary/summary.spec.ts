import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Summary } from './summary';

describe('Summary', () => {
  let component: Summary;
  let fixture: ComponentFixture<Summary>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Summary],
    }).compileComponents();

    fixture = TestBed.createComponent(Summary);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('portfolio', {
      totalCashValue: 5000,
      totalHoldingsValue: 10000,
      holdings: []
    });

    fixture.componentRef.setInput('risk', {
      equity: 15000,
      marginUsed: 2000,
      maintenanceMargin: 1000,
      unrealizedPnl: 500,
      marginRatio: 0.13,
      riskLevel: 'SAFE',
      isUnderLiquidation: false
    });

    fixture.componentRef.setInput('baseCurrency', 'USD');

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should calculate marginUsedPercent correctly', () => {
    expect(component.marginUsedPercent()).toBeCloseTo((2000 / 15000) * 100, 2);
  });
});