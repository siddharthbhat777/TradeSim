import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Ipo } from './ipo';
import { vi } from 'vitest';

describe('Ipo', () => {
  let component: Ipo;
  let fixture: ComponentFixture<Ipo>;

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