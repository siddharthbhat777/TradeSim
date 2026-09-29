import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { signal } from '@angular/core';
import { Workspace } from './workspace';
import { AuthService } from '../services/auth/auth-service';
import { UserService } from '../services/user/user-service';
import { CompanyService } from '../services/company/company-service';
import { DialogService } from '../shared/components/dialog/dialog.service';
import { Role } from '../constants/auth';

describe('Workspace', () => {
  let component: Workspace;
  let fixture: ComponentFixture<Workspace>;
  let authServiceMock: any;
  let userServiceMock: any;
  let companyServiceMock: any;
  let dialogServiceMock: any;
  let router: Router;

  beforeAll(() => {
    vi.stubGlobal('ResizeObserver', vi.fn().mockImplementation(() => ({
      observe: vi.fn(),
      unobserve: vi.fn(),
      disconnect: vi.fn(),
    })));
    vi.stubGlobal('matchMedia', vi.fn().mockImplementation((query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })));
  });

  afterAll(() => {
    vi.unstubAllGlobals();
  });

  beforeEach(async () => {
    authServiceMock = {
      isLoggedIn: vi.fn().mockReturnValue(true),
      currentUser: signal({ role: Role.companyRepresentative, username: 'rep' }),
      logout: vi.fn().mockReturnValue(of(void 0))
    };

    userServiceMock = {
      getProfile: vi.fn().mockReturnValue(of({
        id: 'usr-1',
        fullName: 'Test Rep',
        email: 'rep@test.com',
        role: Role.companyRepresentative
      }))
    };

    companyServiceMock = {
      getAssignedCompanies: vi.fn().mockReturnValue(of([
        { id: 'comp-1', name: 'Test Company 1', primaryContactId: 'usr-1' },
        { id: 'comp-2', name: 'Test Company 2', primaryContactId: 'usr-2' }
      ]))
    };

    dialogServiceMock = {
      open: vi.fn().mockImplementation((config: any) => {
        if (config.onPrimary) config.onPrimary();
      })
    };

    vi.spyOn(Storage.prototype, 'getItem').mockReturnValue(null);
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { });

    await TestBed.configureTestingModule({
      imports: [Workspace],
      providers: [
        provideRouter([
          { path: '**', component: Workspace }
        ]),
        { provide: AuthService, useValue: authServiceMock },
        { provide: UserService, useValue: userServiceMock },
        { provide: CompanyService, useValue: companyServiceMock },
        { provide: DialogService, useValue: dialogServiceMock }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate');

    fixture = TestBed.createComponent(Workspace);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should create the workspace component', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should load profile and assigned companies on init for company representative', () => {
    fixture.detectChanges();

    expect(userServiceMock.getProfile).toHaveBeenCalled();
    expect(companyServiceMock.getAssignedCompanies).toHaveBeenCalled();
    expect(component.userProfile()?.id).toBe('usr-1');
    expect(component.companies().length).toBe(2);
  });

  it('should sync state with url on NavigationEnd', async () => {
    fixture.detectChanges();

    await router.navigateByUrl('/app/representative/comp-2/overview');

    expect(component.currentContext()).toBe('company');
    expect(component.selectedCompanyId()).toBe('comp-2');
    expect(Storage.prototype.setItem).toHaveBeenCalledWith('tradesim_last_company_id', 'comp-2');
  });

  it('should change context to personal and navigate', () => {
    fixture.detectChanges();

    component.onContextChange('personal');

    expect(component.currentContext()).toBe('personal');
    expect(router.navigate).toHaveBeenCalledWith(['/app/portfolio']);
  });

  it('should change context to company, fallback to first company if no targetId, and navigate', () => {
    fixture.detectChanges();

    component.onContextChange('company');

    expect(component.currentContext()).toBe('company');
    expect(component.selectedCompanyId()).toBe('comp-1');
    expect(router.navigate).toHaveBeenCalledWith(['/app/representative', 'comp-1', 'overview']);
  });

  it('should handle company change and navigate', () => {
    fixture.detectChanges();

    component.onCompanyChange('comp-2');

    expect(component.selectedCompanyId()).toBe('comp-2');
    expect(Storage.prototype.setItem).toHaveBeenCalledWith('tradesim_last_company_id', 'comp-2');
    expect(router.navigate).toHaveBeenCalledWith(['/app/representative', 'comp-2', 'overview']);
  });

  it('should toggle collapse state', () => {
    expect(component.isCollapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.isCollapsed()).toBe(true);
  });

  it('should toggle mobile menu state', () => {
    expect(component.isMobileMenuOpen()).toBe(false);
    component.toggleMobileMenu();
    expect(component.isMobileMenuOpen()).toBe(true);
    component.closeMobileMenu();
    expect(component.isMobileMenuOpen()).toBe(false);
  });

  it('should open logout dialog and call logout on confirm', () => {
    fixture.detectChanges();

    component.onLogoutClick();

    expect(dialogServiceMock.open).toHaveBeenCalled();
    expect(authServiceMock.logout).toHaveBeenCalled();
  });
});