import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Logo } from '../shared/components/logo/logo';
import { Tooltip } from '../shared/components/tooltip/tooltip';
import { AuthService } from '../services/auth/auth-service';
import { DialogService } from '../shared/components/dialog/dialog.service';

export interface NavItem {
  label: string;
  route: string;
  icon: string;
}

const USER_MENU: NavItem[] = [
  { label: 'Dashboard', route: 'dashboard', icon: 'layout-dashboard' },
  { label: 'Stock Details', route: 'stock', icon: 'line-chart' },
  { label: 'Portfolio', route: 'portfolio', icon: 'pie-chart' },
  { label: 'Position', route: 'position', icon: 'briefcase' },
  { label: 'IPO Center', route: 'ipo', icon: 'landmark' },
  { label: 'Order', route: 'order', icon: 'shopping-cart' },
  { label: 'Account', route: 'account', icon: 'user' }
];

@Component({
  selector: 'app-workspace',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Logo, Tooltip],
  templateUrl: './workspace.html',
  styleUrl: './workspace.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Workspace {
  readonly menuItems = signal<NavItem[]>(USER_MENU);
  readonly isCollapsed = signal(false);
  readonly isMobileMenuOpen = signal(false);

  private authService = inject(AuthService);
  private dialogService = inject(DialogService);

  toggleCollapse(): void {
    this.isCollapsed.update(v => !v);
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update(v => !v);
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen.set(false);
  }

  onSettingsClick(): void {
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