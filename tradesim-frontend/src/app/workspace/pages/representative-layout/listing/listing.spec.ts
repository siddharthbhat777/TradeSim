import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Listing } from './listing';

describe('Listing', () => {
  let component: Listing;
  let fixture: ComponentFixture<Listing>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Listing],
    }).compileComponents();

    fixture = TestBed.createComponent(Listing);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
