import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ExchangeSlate } from './exchange-slate';

describe('ExchangeSlate', () => {
  let component: ExchangeSlate;
  let fixture: ComponentFixture<ExchangeSlate>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExchangeSlate]
    }).compileComponents();

    fixture = TestBed.createComponent(ExchangeSlate);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('exchanges', []);
    fixture.componentRef.setInput('selectedExchangeId', null);
    fixture.componentRef.setInput('clocks', new Map());
    fixture.componentRef.setInput('stocksCount', 0);
    fixture.componentRef.setInput('indicesCount', 0);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});