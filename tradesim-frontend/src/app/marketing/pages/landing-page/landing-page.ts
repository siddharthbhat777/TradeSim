import { Component, effect, HostListener, inject, signal } from '@angular/core';
import { RouterLink } from "@angular/router";
import { CommonModule } from '@angular/common';
import { Auth } from "../../components/auth/auth";
import { AuthService } from '../../../services/auth/auth-service';
import { AuthStatus } from '../../../constants/auth';
import { Logo } from '../../../shared/components/logo/logo';
import { DialogService } from '../../../shared/components/dialog/dialog.service';
import { Hero } from './hero/hero';
import { Mechanics } from './mechanics/mechanics';
import { Features } from './features/features';
import { Cta } from './cta/cta';

@Component({
  selector: 'app-landing-page',
  imports: [CommonModule, RouterLink, Auth, Logo, Hero, Mechanics, Features, Cta],
  templateUrl: './landing-page.html',
  styleUrl: './landing-page.scss'
})
export class LandingPage {
  showAuth = signal(false);
  readonly authStatus = AuthStatus;

  isMobile = signal(typeof window !== 'undefined' ? window.innerWidth <= 480 : false);

  private readonly authService = inject(AuthService);
  private readonly dialogService = inject(DialogService);

  readonly isLoggedIn = this.authService.isLoggedIn;

  constructor() {
    effect(() => {
      this.showAuth.set(this.authService.showAuthDialog().show);
    }, { allowSignalWrites: true });
  }

  @HostListener('window:resize')
  onResize() {
    this.isMobile.set(window.innerWidth <= 480);
  }

  showAuthDialog(status: AuthStatus) {
    this.authService.showAuthDialog.set({
      show: true,
      status
    });
  }

  logoutUser() {
    this.dialogService.open({
      title: 'Log Out',
      message: 'Are you sure you want to log out of your session?',
      primaryLabel: 'Log Out',
      secondaryLabel: 'Cancel',
      onPrimary: () => {
        this.authService.logout().subscribe();
      }
    });
  }

  closeAuth() {
    this.authService.showAuthDialog.update(authStatus => ({
      ...authStatus,
      show: false
    }));
  }
}