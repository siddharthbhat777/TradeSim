import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  ReactiveFormsModule,
  FormBuilder,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { forkJoin, interval, Subscription } from 'rxjs';

import { Card } from '../../../shared/components/card/card';
import { CustomInput } from '../../../shared/components/input/input';
import { InputDirective } from '../../../shared/directives/input';
import { Button } from '../../../shared/components/button/button';
import { Modal } from '../../../shared/components/modal/modal';
import { FormatCurrencyPipe } from '../../../shared/pipes/format-currency-pipe';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { DialogService } from '../../../shared/components/dialog/dialog.service';
import { AuthService } from '../../../services/auth/auth-service';
import { UserService } from '../../../services/user/user-service';
import { ResetPasswordRequest, SendOtpRequest, UserProfile } from '../../../models/user';
import { OtpPurpose } from '../../../constants/auth';

const PASSWORD_PATTERN = /^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$/;

function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  return control.get('newPassword')?.value === control.get('confirmPassword')?.value
    ? null
    : { passwordMismatch: true };
}

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    Card,
    CustomInput,
    InputDirective,
    Button,
    Modal,
    FormatCurrencyPipe
  ],
  templateUrl: './settings.html',
  styleUrl: './settings.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Settings implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(DialogService);
  private readonly auth = inject(AuthService);

  readonly profile = signal<UserProfile | null>(null);
  readonly baseCurrency = signal('INR');
  readonly isLoading = signal(true);

  readonly isSavingProfile = signal(false);
  readonly isSendingEmailOtp = signal(false);
  readonly isVerifyingEmail = signal(false);
  readonly emailOtpActive = signal(false);
  readonly emailSecondsRemaining = signal(120);

  readonly bankBalance = signal<number | null>(null);
  readonly showBalanceModal = signal(false);
  readonly isRevealingBalance = signal(false);

  readonly securityMode = signal<'standard' | 'forgot'>('standard');
  readonly isChangingPassword = signal(false);
  readonly isSendingForgotOtp = signal(false);
  readonly isResettingPassword = signal(false);
  readonly forgotOtpActive = signal(false);
  readonly forgotSecondsRemaining = signal(120);

  readonly showDeactivateModal = signal(false);
  readonly isDeactivating = signal(false);

  readonly profileForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(100)]],
    linkedBankName: ['', [Validators.required, Validators.maxLength(100)]]
  });

  readonly emailForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
  });

  readonly standardPasswordForm = this.fb.nonNullable.group(
    {
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
      confirmPassword: ['', Validators.required]
    },
    { validators: passwordsMatch }
  );

  readonly forgotPasswordForm = this.fb.nonNullable.group(
    {
      otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
      newPassword: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
      confirmPassword: ['', Validators.required]
    },
    { validators: passwordsMatch }
  );

  readonly balanceForm = this.fb.nonNullable.group({
    password: ['', Validators.required]
  });

  readonly deactivateForm = this.fb.nonNullable.group({
    password: ['', Validators.required]
  });

  readonly emailValue = toSignal(this.emailForm.controls.email.valueChanges, {
    initialValue: ''
  });

  readonly canRequestEmailVerification = computed(() => {
    return !this.emailOtpActive()
      && this.emailForm.controls.email.valid
      && this.emailValue() !== this.profile()?.email;
  });

  private emailTimer?: Subscription;
  private forgotTimer?: Subscription;

  ngOnInit(): void {
    forkJoin({
      profile: this.userService.getProfile(),
      account: this.userService.getTradingAccount()
    }).subscribe({
      next: ({ profile, account }) => {
        this.profile.set(profile);
        this.baseCurrency.set(account.baseCurrency);
        this.profileForm.reset({
          fullName: profile.fullName,
          linkedBankName: profile.linkedBankName
        });
        this.emailForm.reset({ email: profile.email, otp: '' });
        this.isLoading.set(false);
      },
      error: (error) => {
        this.isLoading.set(false);
        this.toast.danger(this.errorMessage(error));
      }
    });
  }

  ngOnDestroy(): void {
    this.emailTimer?.unsubscribe();
    this.forgotTimer?.unsubscribe();
  }

  saveProfile(): void {
    if (this.profileForm.invalid || this.profileForm.pristine) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.isSavingProfile.set(true);

    this.userService.updateProfile(this.profileForm.getRawValue()).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.profileForm.reset({
          fullName: profile.fullName,
          linkedBankName: profile.linkedBankName
        });
        this.isSavingProfile.set(false);
        this.toast.success('Profile changes saved.');
      },
      error: (error) => {
        this.isSavingProfile.set(false);
        this.toast.danger(this.errorMessage(error));
      }
    });
  }

  requestEmailOtp(): void {
    if (!this.canRequestEmailVerification()) {
      return;
    }

    this.isSendingEmailOtp.set(true);

    this.userService.initiateEmailChange(this.emailForm.controls.email.value).subscribe({
      next: () => {
        this.emailOtpActive.set(true);
        this.emailForm.controls.email.disable();
        this.startEmailTimer();
        this.isSendingEmailOtp.set(false);
        this.toast.success('OTP has been sent to your new email address.');
      },
      error: (error) => {
        this.isSendingEmailOtp.set(false);
        this.setServerError(this.emailForm.controls.email, this.errorMessage(error));
      }
    });
  }

  confirmEmailChange(): void {
    if (this.emailForm.controls.otp.invalid) {
      this.emailForm.controls.otp.markAsTouched();
      return;
    }

    this.isVerifyingEmail.set(true);

    this.userService.verifyEmailChange({
      newEmail: this.emailForm.getRawValue().email,
      otp: this.emailForm.getRawValue().otp
    }).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.cancelEmailChange(false);
        this.isVerifyingEmail.set(false);
        this.toast.success('Email address verified and updated.');
      },
      error: (error) => {
        this.isVerifyingEmail.set(false);
        this.setServerError(this.emailForm.controls.otp, this.errorMessage(error));
      }
    });
  }

  cancelEmailChange(showToast = true): void {
    this.emailTimer?.unsubscribe();
    this.emailOtpActive.set(false);
    this.emailSecondsRemaining.set(120);
    this.emailForm.controls.email.enable();
    this.emailForm.reset({ email: this.profile()?.email ?? '', otp: '' });

    if (showToast) {
      this.toast.info('Email change cancelled.');
    }
  }

  openBalanceModal(): void {
    this.balanceForm.reset();
    this.showBalanceModal.set(true);
  }

  closeBalanceModal(): void {
    this.balanceForm.reset();
    this.showBalanceModal.set(false);
  }

  revealBalance(): void {
    if (this.balanceForm.invalid) {
      this.balanceForm.markAllAsTouched();
      return;
    }

    this.isRevealingBalance.set(true);

    this.userService.revealBankBalance(this.balanceForm.controls.password.value).subscribe({
      next: ({ bankBalance }) => {
        this.bankBalance.set(bankBalance);
        this.closeBalanceModal();
        this.isRevealingBalance.set(false);
      },
      error: (error) => {
        this.isRevealingBalance.set(false);
        this.setServerError(this.balanceForm.controls.password, this.errorMessage(error));
      }
    });
  }

  openForgotPassword(): void {
    this.securityMode.set('forgot');
    this.standardPasswordForm.reset();
  }

  returnToStandardPassword(): void {
    this.forgotTimer?.unsubscribe();
    this.forgotOtpActive.set(false);
    this.forgotSecondsRemaining.set(120);
    this.forgotPasswordForm.reset();
    this.securityMode.set('standard');
  }

  changePassword(): void {
    if (this.standardPasswordForm.invalid) {
      this.standardPasswordForm.markAllAsTouched();
      return;
    }
    this.isChangingPassword.set(true);

    this.userService.changePassword(this.standardPasswordForm.getRawValue()).subscribe({
      next: () => {
        this.standardPasswordForm.reset();
        this.isChangingPassword.set(false);
        this.toast.success('Password updated successfully.');
      },
      error: (error) => {
        this.isChangingPassword.set(false);
        this.setServerError(
          this.standardPasswordForm.controls.currentPassword,
          this.errorMessage(error)
        );
      }
    });
  }

  sendForgotPasswordOtp(): void {
    const email = this.profile()?.email;
    if (!email) return;

    this.isSendingForgotOtp.set(true);

    const otpRequest: SendOtpRequest = {
      email,
      purpose: OtpPurpose.FORGOT_PASSWORD
    }

    this.authService.requestOtp(otpRequest).subscribe({
      next: () => {
        this.forgotOtpActive.set(true);
        this.startForgotTimer();
        this.isSendingForgotOtp.set(false);
        this.toast.success('OTP has been sent to your email.');
      },
      error: (error) => {
        this.isSendingForgotOtp.set(false);
        this.toast.danger(this.errorMessage(error));
      }
    });
  }

  resetPassword(): void {
    if (this.forgotPasswordForm.invalid || !this.profile()?.email) {
      this.forgotPasswordForm.markAllAsTouched();
      return;
    }

    const { otp, newPassword } = this.forgotPasswordForm.getRawValue();
    this.isResettingPassword.set(true);

    const resetPasswordRequest: ResetPasswordRequest = {
      email: this.profile()!.email,
      otp,
      newPassword
    }

    this.authService.resetPassword(resetPasswordRequest).subscribe({
      next: () => {
        this.forgotTimer?.unsubscribe();
        this.forgotOtpActive.set(false);
        this.forgotPasswordForm.reset();
        this.isResettingPassword.set(false);
        this.toast.success('Password reset successfully.');
        this.securityMode.set('standard');
      },
      error: (error) => {
        this.isResettingPassword.set(false);
        this.setServerError(this.forgotPasswordForm.controls.otp, this.errorMessage(error));
      }
    });
  }

  requestDeactivation(): void {
    this.dialog.open({
      title: 'Deactivate your account?',
      message: 'This cancels open orders and logs you out immediately.',
      primaryLabel: 'Proceed',
      secondaryLabel: 'Cancel',
      primaryVariant: 'danger',
      isBlocking: true,
      onPrimary: () => {
        this.deactivateForm.reset();
        this.showDeactivateModal.set(true);
      }
    });
  }

  closeDeactivateModal(): void {
    this.deactivateForm.reset();
    this.showDeactivateModal.set(false);
  }

  deactivateAccount(): void {
    if (this.deactivateForm.invalid) {
      this.deactivateForm.markAllAsTouched();
      return;
    }

    this.isDeactivating.set(true);

    this.authService.deactivateAccount(this.deactivateForm.controls.password.value).subscribe({
      next: () => {
        this.closeDeactivateModal();
        this.auth.clearSession();
      },
      error: (error) => {
        this.isDeactivating.set(false);
        this.setServerError(this.deactivateForm.controls.password, this.errorMessage(error));
      }
    });
  }

  inputError(control: AbstractControl, type: 'text' | 'email' | 'otp' | 'password' | 'confirm'): string {
    if (!control.invalid || !(control.touched || control.dirty)) {
      return '';
    }

    if (control.errors?.['server']) return String(control.errors['server']);
    if (control.errors?.['required']) return 'This field is required.';
    if (control.errors?.['email']) return 'Enter a valid email address.';
    if (control.errors?.['maxlength']) return 'Maximum 100 characters allowed.';
    if (type === 'otp') return 'Enter the six-digit OTP.';
    if (type === 'password') {
      return 'Use 8+ characters with uppercase, number, and special character.';
    }
    if (type === 'confirm') return 'Passwords do not match.';

    return 'Enter a valid value.';
  }

  clearServerError(control: AbstractControl): void {
    if (!control.errors?.['server']) return;

    const errors = { ...control.errors };
    delete errors['server'];
    control.setErrors(Object.keys(errors).length ? errors : null);
  }

  private setServerError(control: AbstractControl, message: string): void {
    control.setErrors({ ...(control.errors ?? {}), server: message });
    control.markAsTouched();
  }

  private errorMessage(error: unknown): string {
    const httpError = error as HttpErrorResponse;
    return httpError.error?.message ?? 'Something went wrong. Please try again.';
  }

  private startEmailTimer(): void {
    this.emailTimer?.unsubscribe();
    this.emailSecondsRemaining.set(120);

    this.emailTimer = interval(1000).subscribe(() => {
      const remaining = this.emailSecondsRemaining() - 1;

      if (remaining <= 0) {
        this.cancelEmailChange(false);
        this.toast.warning('OTP expired. Request a new verification code.');
        return;
      }

      this.emailSecondsRemaining.set(remaining);
    });
  }

  private startForgotTimer(): void {
    this.forgotTimer?.unsubscribe();
    this.forgotSecondsRemaining.set(120);

    this.forgotTimer = interval(1000).subscribe(() => {
      const remaining = this.forgotSecondsRemaining() - 1;

      if (remaining <= 0) {
        this.forgotTimer?.unsubscribe();
        this.forgotOtpActive.set(false);
        this.forgotPasswordForm.reset();
        this.toast.warning('OTP expired. Request a new code.');
        return;
      }

      this.forgotSecondsRemaining.set(remaining);
    });
  }
}