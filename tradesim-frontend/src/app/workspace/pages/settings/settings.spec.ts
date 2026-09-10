import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Settings } from './settings';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('Settings', () => {
  let component: Settings;
  let fixture: ComponentFixture<Settings>;
  let toastServiceSpy: any;

  beforeEach(async () => {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation(query => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    });

    toastServiceSpy = {
      info: vi.fn(),
      warning: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [Settings],
      providers: [
        { provide: ToastService, useValue: toastServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Settings);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should switch tabs', () => {
    component.setTab('security');
    expect(component.activeTab()).toBe('security');
  });

  it('should toggle faq', () => {
    component.toggleFaq(1);
    expect(component.expandedFaqIndex()).toBe(1);
    component.toggleFaq(1);
    expect(component.expandedFaqIndex()).toBeNull();
  });

  it('should show toast on save profile', () => {
    component.onSaveProfile();
    expect(toastServiceSpy.info).toHaveBeenCalled();
  });

  it('should show toast on save preferences', () => {
    component.onSavePreferences();
    expect(toastServiceSpy.info).toHaveBeenCalled();
  });

  it('should show warning toast on deactivate account', () => {
    component.onDeactivateAccount();
    expect(toastServiceSpy.warning).toHaveBeenCalled();
  });
});