import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Position } from './position';

describe('Position', () => {
  let component: Position;
  let fixture: ComponentFixture<Position>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Position],
    }).compileComponents();

    fixture = TestBed.createComponent(Position);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
