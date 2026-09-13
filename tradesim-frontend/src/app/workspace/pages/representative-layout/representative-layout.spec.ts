import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RepresentativeLayout } from './representative-layout';

describe('RepresentativeLayout', () => {
  let component: RepresentativeLayout;
  let fixture: ComponentFixture<RepresentativeLayout>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RepresentativeLayout],
    }).compileComponents();

    fixture = TestBed.createComponent(RepresentativeLayout);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
