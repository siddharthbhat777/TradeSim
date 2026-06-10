import { Component, effect, inject, signal } from '@angular/core';
import { RouterLink } from "@angular/router";
import { Auth } from "../../components/auth/auth";
import { AuthService } from '../../../services/auth-service/auth-service';
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
  readonly authStatus = AuthStatus;

  private authService = inject(AuthService);
  readonly userDetails = this.authService.currentUser;
  readonly isLoggedIn = this.authService.isLoggedIn;

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

  logoutUser() {
    this.authService.logout().subscribe({
      error: (error) => {
        console.log(error.message);
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