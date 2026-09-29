import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Team } from './team';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { FormBuilder } from '@angular/forms';
import { CompanyService } from '../../../../services/company/company-service';
import { UserService } from '../../../../services/user/user-service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { DialogService } from '../../../../shared/components/dialog/dialog.service';
import { describe, it, expect, beforeEach, vi } from 'vitest';

class ResizeObserverMock {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
}

describe('Team', () => {
  let component: Team;
  let fixture: ComponentFixture<Team>;
  let companyServiceSpy: any;
  let userServiceSpy: any;
  let toastServiceSpy: any;
  let dialogServiceSpy: any;

  const mockProfile = {
    id: 'usr-1',
    fullName: 'Primary User',
    email: 'primary@test.com',
    role: 'COMPANY_REPRESENTATIVE'
  };

  const mockReps = [
    {
      id: 'assignment-1',
      companyId: 'cmp-1',
      userId: 'usr-1',
      fullName: 'Primary User',
      email: 'primary@test.com',
      assignmentRole: 'PRIMARY_CONTACT',
      status: 'ACTIVE'
    },
    {
      id: 'assignment-2',
      companyId: 'cmp-1',
      userId: 'usr-2',
      fullName: 'Manager User',
      email: 'manager@test.com',
      assignmentRole: 'MANAGER',
      status: 'ACTIVE'
    }
  ];

  const mockEligibleUsers = [
    {
      id: 'usr-1',
      fullName: 'Primary User',
      username: 'primary',
      email: 'primary@test.com',
      accountStatus: 'ACTIVE'
    },
    {
      id: 'usr-3',
      fullName: 'New Rep',
      username: 'newrep',
      email: 'newrep@test.com',
      accountStatus: 'ACTIVE'
    }
  ];

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

    vi.stubGlobal('ResizeObserver', ResizeObserverMock);

    Object.assign(navigator, {
      clipboard: {
        writeText: vi.fn().mockImplementation(() => Promise.resolve()),
      },
    });

    companyServiceSpy = {
      getRepresentatives: vi.fn().mockReturnValue(of(mockReps)),
      getEligibleRepresentatives: vi.fn().mockReturnValue(of(mockEligibleUsers)),
      assignRepresentative: vi.fn().mockReturnValue(of({})),
      revokeRepresentative: vi.fn().mockReturnValue(of({})),
      transferPrimaryContact: vi.fn().mockReturnValue(of({}))
    };

    userServiceSpy = {
      getProfile: vi.fn().mockReturnValue(of(mockProfile))
    };

    toastServiceSpy = {
      success: vi.fn(),
      danger: vi.fn()
    };

    dialogServiceSpy = {
      open: vi.fn().mockImplementation((config: any) => {
        if (config.onPrimary) config.onPrimary();
      })
    };

    await TestBed.configureTestingModule({
      imports: [Team],
      providers: [
        FormBuilder,
        { provide: CompanyService, useValue: companyServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
        { provide: DialogService, useValue: dialogServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            parent: { snapshot: { paramMap: { get: () => 'cmp-1' } } },
            snapshot: { paramMap: { get: () => null } }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Team);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load team data and eligible users on init', () => {
    expect(component.companyId()).toBe('cmp-1');
    expect(companyServiceSpy.getRepresentatives).toHaveBeenCalledWith('cmp-1');
    expect(companyServiceSpy.getEligibleRepresentatives).toHaveBeenCalled();
    expect(component.representatives().length).toBe(2);
    expect(component.eligibleUsers().length).toBe(2);
    expect(component.isPrimaryContact()).toBe(true);
    expect(component.activeRepsCount()).toBe(2);
  });

  it('should format available user options and mark correctly', () => {
    const options = component.availableUsersOptions();
    expect(options.length).toBe(2);

    const assignedOption = options.find(o => o.value === 'usr-1');
    expect(assignedOption?.isEligible).toBe(false);
    expect(assignedOption?.statusText).toBe('Already Assigned');

    const eligibleOption = options.find(o => o.value === 'usr-3');
    expect(eligibleOption?.isEligible).toBe(true);
    expect(eligibleOption?.statusText).toBe('Eligible');
  });

  it('should open and close add modal', () => {
    component.openAddModal();
    expect(component.showAddModal()).toBe(true);

    component.closeAddModal();
    expect(component.showAddModal()).toBe(false);
  });

  it('should submit add representative', () => {
    component.addRepForm.setValue({ userId: 'usr-3' });
    component.submitAddRep();

    expect(companyServiceSpy.assignRepresentative).toHaveBeenCalledWith('cmp-1', 'usr-3');
    expect(toastServiceSpy.success).toHaveBeenCalled();
  });

  it('should trigger transfer confirmation dialog', () => {
    component.confirmTransfer('usr-2', 'Manager User');

    expect(dialogServiceSpy.open).toHaveBeenCalled();
    expect(companyServiceSpy.transferPrimaryContact).toHaveBeenCalledWith('cmp-1', 'usr-2');
  });

  it('should trigger revoke confirmation dialog', () => {
    component.confirmRevoke('usr-2', 'Manager User');

    expect(dialogServiceSpy.open).toHaveBeenCalled();
    expect(companyServiceSpy.revokeRepresentative).toHaveBeenCalledWith('cmp-1', 'usr-2');
  });

  it('should copy user id to clipboard', async () => {
    component.copyId('usr-123');
    expect(navigator.clipboard.writeText).toHaveBeenCalledWith('usr-123');
  });
});