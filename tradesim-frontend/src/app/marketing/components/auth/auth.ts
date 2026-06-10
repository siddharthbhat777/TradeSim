import { Component, inject, output, signal } from '@angular/core';
import { Modal } from "../../../shared/components/modal/modal";
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RegisterRequest } from '../../../models/register-request';
import { AuthService } from '../../../services/auth-service/auth-service';
import { AuthStatus } from '../../../constants/auth';
import { LoginRequest } from '../../../models/login-request';

@Component({
  selector: 'app-auth',
  imports: [Modal, ReactiveFormsModule],
  templateUrl: './auth.html',
  styleUrl: './auth.scss'
})
export class Auth {
  showAuth = output();
  readonly authStatus = AuthStatus;
  currentAuthStatus = signal(AuthStatus.Login);

  private readonly fb = inject(NonNullableFormBuilder);
  private authService = inject(AuthService);

  constructor() {
    this.currentAuthStatus.set(this.authService.showAuthDialog().status);
  }

  loginForm = this.fb.group({
    usernameOrEmail: ['', Validators.required],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  loginSubmit() {
    if (this.loginForm.invalid) {
      return;
    }
    const formData: LoginRequest = this.loginForm.getRawValue();

    this.authService.loginUser(formData).subscribe({
      next: () => {
        this.loginForm.reset();
        this.showAuth.emit();
      },
      error: (error) => {
        console.log(error.message);
      }
    });
  }

  registerForm = this.fb.group({
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  registerSubmit() {
    if (this.registerForm.invalid) {
      return;
    }
    const formData: RegisterRequest = this.registerForm.getRawValue();

    this.authService.registerUser(formData).subscribe({
      next: () => {
        this.registerForm.reset();
        this.closeAuth();
      },
      error: (error) => {
        console.log(error.message);
      }
    });
  }

  closeAuth() {
    this.showAuth.emit();
  }
}