import { Component, effect, inject, signal } from '@angular/core';
import { RouterLink } from "@angular/router";
import { Auth } from "../../components/auth/auth";
import { AuthService } from '../../../services/auth/auth';
import { AuthStatus } from '../../../constants/auth';

@Component({
  selector: 'app-landing-page',
  imports: [RouterLink, Auth],
  templateUrl: './landing-page.html',
  styleUrl: './landing-page.scss',
})
export class LandingPage {
  showAuth = signal(false);
  isLoginSelected = signal(false);
  isLoggedIn = signal(false);
  readonly authStatus = AuthStatus;

  private authService = inject(AuthService);

  constructor() {
    effect(() => {
      this.showAuth.set(this.authService.showAuthDialog().show);
    });
  }

  showAuthDialog(status: AuthStatus) {
    this.authService.showAuthDialog.set({
      show: true,
      status
    });
  }

  closeAuth() {
    this.authService.showAuthDialog.update(authStatus => ({
      ...authStatus,
      show: false
    }));
  }
}