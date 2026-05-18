import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Ipo } from './ipo';

describe('Ipo', () => {
  let component: Ipo;
  let fixture: ComponentFixture<Ipo>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Ipo],
    }).compileComponents();

    fixture = TestBed.createComponent(Ipo);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
