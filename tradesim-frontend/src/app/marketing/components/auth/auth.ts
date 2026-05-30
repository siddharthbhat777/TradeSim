import { Component, inject, input, output, signal } from '@angular/core';
import { Modal } from "../../../shared/components/modal/modal";
import { FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Register } from '../../../models/register';
import { AuthService } from '../../../services/auth/auth';
import { AuthStatus } from '../../../constants/auth';

@Component({
  selector: 'app-auth',
  imports: [Modal, ReactiveFormsModule],
  templateUrl: './auth.html',
  styleUrl: './auth.scss',
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

  registerForm = this.fb.group({
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  registerSubmit() {
    if (this.registerForm.invalid) {
      return;
    }
    const formData: Register = this.registerForm.getRawValue();

    this.authService.registerUser(formData).subscribe({
      error: (error) => {
        console.log(error.message);
      },
      complete: () => {
        this.registerForm.reset();
        this.showAuth.emit();
      }
    });
  }

  closeAuth() {
    this.showAuth.emit();
  }
}