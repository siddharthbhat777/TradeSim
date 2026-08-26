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

    fixture.componentRef.setInput('equity', 15000);
    fixture.componentRef.setInput('totalInvested', 12000);
    fixture.componentRef.setInput('unrealizedPnl', 3000);
    fixture.componentRef.setInput('buyingPower', 5000);
    fixture.componentRef.setInput('currency', 'INR');

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});