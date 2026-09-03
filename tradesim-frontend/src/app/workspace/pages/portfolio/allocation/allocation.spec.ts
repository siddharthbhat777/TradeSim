import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Allocation } from './allocation';

describe('Allocation', () => {
  let component: Allocation;
  let fixture: ComponentFixture<Allocation>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Allocation],
    }).compileComponents();

    fixture = TestBed.createComponent(Allocation);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('data', []);

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should handle empty data correctly', () => {
    fixture.componentRef.setInput('data', []);
    fixture.detectChanges();
    expect(component.data().length).toBe(0);
  });

  it('should handle populated pie chart data correctly', () => {
    const mockData = [
      { id: '1', label: 'Cash', value: 5000, color: '#000000' },
      { id: '2', label: 'AAPL', value: 2000, color: '#FFFFFF' }
    ];

    fixture.componentRef.setInput('data', mockData);
    fixture.detectChanges();

    expect(component.data().length).toBe(2);
    expect(component.data()[0].label).toBe('Cash');
    expect(component.data()[1].value).toBe(2000);
  });
});