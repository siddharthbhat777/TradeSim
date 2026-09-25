import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { NgTemplateOutlet } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { filter } from 'rxjs/operators';
import { Logo } from '../shared/components/logo/logo';
import { Tooltip } from '../shared/components/tooltip/tooltip';
import { Drawer } from '../shared/components/drawer/drawer';
import { SegmentedControl, SegmentOption } from '../shared/components/segmented-control/segmented-control';
import { Dropdown, DropdownOption } from '../shared/components/dropdown/dropdown';
import { AuthService } from '../services/auth/auth-service';
import { DialogService } from '../shared/components/dialog/dialog.service';
import { CompanyService } from '../services/company/company-service';
import { UserService } from '../services/user/user-service';
import { Role } from '../constants/auth';
import { CompanyResponse } from '../models/company';
import { UserProfile } from '../models/user';

export interface NavItem {
  label: string;
  route: string;
  icon: string;
}

const USER_MENU: NavItem[] = [
  { label: 'Portfolio', route: 'portfolio', icon: 'pie-chart' },
  { label: 'Market', route: 'market', icon: 'line-chart' },
  { label: 'Order', route: 'order', icon: 'shopping-cart' },
  { label: 'IPO Center', route: 'ipo', icon: 'landmark' },
  { label: 'Wallet', route: 'wallet', icon: 'wallet' }
];

const ADMIN_MENU: NavItem[] = [
  { label: 'Dashboard', route: 'admin/dashboard', icon: 'grid' },
  { label: 'Approvals', route: 'admin/approvals', icon: 'check-square' },
  { label: 'Market Ops', route: 'admin/market', icon: 'activity' },
  { label: 'Companies', route: 'admin/companies', icon: 'building' },
  { label: 'Users', route: 'admin/users', icon: 'users' }
];

@Component({
  selector: 'app-workspace',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    FormsModule,
    Logo,
    Tooltip,
    Drawer,
    SegmentedControl,
    Dropdown,
    NgTemplateOutlet
  ],
  templateUrl: './workspace.html',
  styleUrl: './workspace.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Workspace implements OnInit {
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private dialogService = inject(DialogService);
  private companyService = inject(CompanyService);
  private router = inject(Router);

  readonly isCollapsed = signal(false);
  readonly isMobileMenuOpen = signal(false);
  readonly currentContext = signal<'personal' | 'company'>('personal');

  readonly userProfile = signal<UserProfile | null>(null);
  readonly companies = signal<CompanyResponse[]>([]);
  readonly selectedCompanyId = signal<string | null>(null);

  readonly contextOptions: SegmentOption<string>[] = [
    { label: 'Personal', value: 'personal' },
    { label: 'Company', value: 'company' }
  ];

  readonly isCompanyRepresentative = computed(() => this.authService.currentUser()?.role === Role.companyRepresentative);

  readonly companyOptions = computed<DropdownOption<string>[]>(() => {
    const profile = this.userProfile();
    return this.companies().map(c => ({
      label: c.primaryContactId === profile?.id ? `${c.name} (PC)` : c.name,
      value: c.id
    }));
  });

  readonly menuItems = computed(() => {
    const role = this.authService.currentUser()?.role;

    if (role === Role.admin) {
      return ADMIN_MENU;
    }

    if (role === Role.companyRepresentative && this.currentContext() === 'company') {
      const companyId = this.selectedCompanyId();
      if (!companyId) return [];

      return [
        { label: 'Overview', route: `representative/${companyId}/overview`, icon: 'grid' },
        { label: 'Listing', route: `representative/${companyId}/listing`, icon: 'building' },
        { label: 'IPO', route: `representative/${companyId}/ipo`, icon: 'landmark' },
        { label: 'Team', route: `representative/${companyId}/team`, icon: 'users' }
      ];
    }

    return USER_MENU;
  });

  constructor() {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.syncStateWithUrl();
    });
  }

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.userService.getProfile().subscribe(profile => {
        this.userProfile.set(profile);
        if (this.isCompanyRepresentative()) {
          this.companyService.getAssignedCompanies().subscribe(companies => {
            this.companies.set(companies);
            this.syncStateWithUrl();
          });
        } else {
          this.syncStateWithUrl();
        }
      });
    }
  }

  private syncStateWithUrl(): void {
    const url = this.router.url;

    if (url.includes('/representative')) {
      this.currentContext.set('company');

      if (this.companies().length > 0) {
        const idFromUrl = this.extractCompanyIdFromUrl();
        let targetId = idFromUrl;

        if (!targetId || !this.companies().some(c => c.id === targetId)) {
          targetId = localStorage.getItem('tradesim_last_company_id');
        }

        if (!targetId || !this.companies().some(c => c.id === targetId)) {
          targetId = this.companies()[0].id;
        }

        this.selectedCompanyId.set(targetId);
        localStorage.setItem('tradesim_last_company_id', targetId!);

        if (!idFromUrl || idFromUrl !== targetId) {
          this.router.navigate(['/app/representative', targetId, 'overview'], { replaceUrl: true });
        }
      }
    } else {
      this.currentContext.set('personal');
    }
  }

  private extractCompanyIdFromUrl(): string | null {
    const segments = this.router.url.split('/');
    const repIndex = segments.indexOf('representative');
    if (repIndex !== -1 && segments.length > repIndex + 1) {
      const id = segments[repIndex + 1];
      if (id && !['overview', 'listing', 'ipo', 'team'].includes(id)) {
        return id;
      }
    }
    return null;
  }

  onContextChange(value: 'personal' | 'company'): void {
    if (!value) return;
    this.currentContext.set(value);
    this.closeMobileMenu();

    if (value === 'company') {
      let targetId = this.selectedCompanyId();
      if (!targetId) {
        targetId = localStorage.getItem('tradesim_last_company_id');
      }
      if (!targetId && this.companies().length > 0) {
        targetId = this.companies()[0].id;
      }

      if (targetId) {
        this.selectedCompanyId.set(targetId);
        this.router.navigate(['/app/representative', targetId, 'overview']);
      }
    } else {
      this.router.navigate(['/app/portfolio']);
    }
  }

  onCompanyChange(id: string | null): void {
    if (!id) return;
    this.selectedCompanyId.set(id);
    localStorage.setItem('tradesim_last_company_id', id);
    this.router.navigate(['/app/representative', id, 'overview']);
    this.closeMobileMenu();
  }

  toggleCollapse(): void {
    this.isCollapsed.update(v => !v);
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update(v => !v);
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen.set(false);
  }

  onLogoutClick(): void {
    this.dialogService.open({
      title: 'Confirm Logout',
      message: 'Are you sure you want to log out of your account?',
      primaryLabel: 'Logout',
      primaryVariant: 'danger',
      secondaryLabel: 'Cancel',
      isBlocking: true,
      showClose: true,
      onPrimary: () => {
        this.authService.logout().subscribe({
          error: (error) => {
            console.log(error.message);
          }
        });
      }
    });
  }
}