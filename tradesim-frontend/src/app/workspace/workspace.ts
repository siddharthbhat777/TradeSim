import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { NgTemplateOutlet } from '@angular/common';
import { Logo } from '../shared/components/logo/logo';
import { Tooltip } from '../shared/components/tooltip/tooltip';
import { AuthService } from '../services/auth/auth-service';
import { DialogService } from '../shared/components/dialog/dialog.service';
import { Drawer } from '../shared/components/drawer/drawer';
import { Role } from '../constants/auth';

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
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Logo, Tooltip, Drawer, NgTemplateOutlet],
  templateUrl: './workspace.html',
  styleUrl: './workspace.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Workspace {
  private authService = inject(AuthService);
  private dialogService = inject(DialogService);

  readonly isCollapsed = signal(false);
  readonly isMobileMenuOpen = signal(false);

  readonly menuItems = computed(() => {
    const role = this.authService.currentUser()?.role;
    if (role === Role.admin) {
      return ADMIN_MENU;
    }
    return USER_MENU;
  });

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