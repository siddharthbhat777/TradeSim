import { ChangeDetectionStrategy, Component, computed, ElementRef, inject, OnDestroy, OnInit, output, signal, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Subscription, timer, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../../services/auth/auth-service';
import { ForexService, Country } from '../../../services/forex/forex-service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { AuthStatus } from '../../../constants/auth';
import { RegisterRequest } from '../../../models/register-request';
import { LoginRequest } from '../../../models/login-request';

const PASSWORD_REGEX = /^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$\%^&*]).{8,}$/;

const passwordMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password');
  const confirmPassword = control.get('confirmPassword');
  return password && confirmPassword && password.value === confirmPassword.value ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-auth',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './auth.html',
  styleUrl: './auth.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Auth implements OnInit, OnDestroy {
  showAuth = output();

  readonly authStatus = AuthStatus;
  readonly currentAuthStatus = signal<AuthStatus>(AuthStatus.Login);

  readonly isSendingOtp = signal(false);
  readonly isOtpSent = signal(false);
  readonly isProcessing = signal(false);

  readonly otpTimeRemaining = signal(0);
  readonly isOtpExpired = signal(true);
  private otpTimerSub?: Subscription;

  readonly countrySearchInputRef = viewChild<ElementRef<HTMLInputElement>>('countrySearchInput');

  readonly isCountryDropdownOpen = signal(false);
  readonly isCountryDropdownUp = signal(false);
  readonly countrySearchQuery = signal('');
  readonly activeCountryDropdownIndex = signal(-1);

  readonly supportedCurrencies = signal<{ label: string, value: string }[]>([]);
  readonly isCurrencyDropdownOpen = signal(false);
  readonly isCurrencyDropdownUp = signal(false);
  readonly activeDropdownIndex = signal(-1);

  readonly needsBaseCurrencySelection = signal(false);
  readonly isResolvingCurrency = signal(false);

  private readonly fb = inject(NonNullableFormBuilder);
  private readonly authService = inject(AuthService);
  readonly forexService = inject(ForexService);
  private readonly toast = inject(ToastService);

  readonly filteredCountries = computed(() => {
    const query = this.countrySearchQuery().toLowerCase().trim();
    const all = this.forexService.countries();
    if (!query) {
      return all;
    }
    return all.filter(c => c.name.toLowerCase().includes(query) || c.code.toLowerCase().includes(query));
  });

  constructor() {
    this.currentAuthStatus.set(this.authService.showAuthDialog().status);
  }

  ngOnInit() {
    this.forexService.loadCountries();

    this.forexService.getSupportedCurrencies().subscribe({
      next: (currencies) => {
        const sortedOptions = currencies
          .sort((a, b) => a.localeCompare(b))
          .map(currency => ({ label: currency, value: currency }));
        this.supportedCurrencies.set(sortedOptions);
      },
      error: () => {
        this.supportedCurrencies.set([]);
      }
    });
  }

  ngOnDestroy() {
    this.otpTimerSub?.unsubscribe();
  }

  readonly loginForm = this.fb.group({
    usernameOrEmail: ['', Validators.required],
    password: ['', Validators.required],
  });

  loginSubmit() {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isProcessing.set(true);
    const formData: LoginRequest = this.loginForm.getRawValue();

    this.authService.loginUser(formData).subscribe({
      next: () => {
        this.isProcessing.set(false);
        this.loginForm.reset();
        this.closeAuth();
      },
      error: (error) => {
        this.isProcessing.set(false);
        if (error instanceof HttpErrorResponse && error.error?.errorCode === 'AUTH_ACCOUNT_DEACTIVATED') {
          this.currentAuthStatus.set(AuthStatus.Reactivate);
        } else {
          this.toast.danger(error.error?.message || 'Invalid credentials. Please try again.');
        }
      }
    });
  }

  reactivateSubmit() {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isProcessing.set(true);
    const formData: LoginRequest = this.loginForm.getRawValue();

    this.authService.reactivateAccount(formData).subscribe({
      next: () => {
        this.isProcessing.set(false);
        this.toast.success('Welcome back! Your account has been reactivated.');
        this.loginForm.reset();
        this.closeAuth();
      },
      error: (error) => {
        this.isProcessing.set(false);
        this.toast.danger(error.error?.message || 'Failed to reactivate account.');
      }
    });
  }

  readonly registerForm = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    username: ['', Validators.required],
    linkedBankName: ['', Validators.required],
    password: ['', [Validators.required, Validators.pattern(PASSWORD_REGEX)]],
    confirmPassword: ['', Validators.required],
    countryCode: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]],
    baseCurrency: [''],
    email: ['', [Validators.required, Validators.email]],
    otp: [{ value: '', disabled: true }, [Validators.required, Validators.minLength(6), Validators.maxLength(6), Validators.pattern('^[0-9]*$')]]
  }, { validators: passwordMatchValidator });

  toggleCountryDropdown(event?: Event) {
    const willOpen = !this.isCountryDropdownOpen();
    this.isCountryDropdownOpen.set(willOpen);

    if (willOpen) {
      this.isCurrencyDropdownOpen.set(false);
      this.countrySearchQuery.set('');
      const currentVal = this.registerForm.controls.countryCode.value;
      const idx = this.filteredCountries().findIndex(c => c.code === currentVal);
      this.activeCountryDropdownIndex.set(idx >= 0 ? idx : 0);

      if (event && event.currentTarget) {
        const trigger = event.currentTarget as HTMLElement;
        const modal = trigger.closest('.auth-modal');
        if (modal) {
          const triggerRect = trigger.getBoundingClientRect();
          const modalRect = modal.getBoundingClientRect();
          const spaceBelow = modalRect.bottom - triggerRect.bottom;
          const spaceAbove = triggerRect.top - modalRect.top;
          this.isCountryDropdownUp.set(spaceBelow < 280 && spaceAbove > spaceBelow);
        } else {
          const rect = trigger.getBoundingClientRect();
          const spaceBelow = window.innerHeight - rect.bottom;
          this.isCountryDropdownUp.set(spaceBelow < 280 && rect.top > 280);
        }
      }

      queueMicrotask(() => this.countrySearchInputRef()?.nativeElement.focus());
    } else {
      this.registerForm.controls.countryCode.markAsTouched();
    }
  }

  closeCountryDropdown() {
    this.isCountryDropdownOpen.set(false);
    this.registerForm.controls.countryCode.markAsTouched();
  }

  selectCountry(country: Country) {
    this.registerForm.controls.countryCode.setValue(country.code);
    this.registerForm.controls.countryCode.markAsTouched();
    this.closeCountryDropdown();
    this.resolveCurrencyForCountry(country.code);
  }

  onCountrySearchInput(event: Event) {
    const query = (event.target as HTMLInputElement).value;
    this.countrySearchQuery.set(query);
    this.activeCountryDropdownIndex.set(0);
  }

  onCountryKeydown(event: KeyboardEvent) {
    if (!this.isCountryDropdownOpen()) {
      if (event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        this.toggleCountryDropdown(event);
      }
      return;
    }
  }

  onCountrySearchKeydown(event: KeyboardEvent) {
    const items = this.filteredCountries();
    const maxIdx = items.length - 1;

    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.activeCountryDropdownIndex.update(i => Math.min(i + 1, maxIdx));
        this.scrollToActiveOption('.country-options-list', '.custom-dropdown-option.active');
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.activeCountryDropdownIndex.update(i => Math.max(i - 1, 0));
        this.scrollToActiveOption('.country-options-list', '.custom-dropdown-option.active');
        break;
      case 'Enter':
        event.preventDefault();
        const active = this.activeCountryDropdownIndex();
        if (active >= 0 && active <= maxIdx) {
          this.selectCountry(items[active]);
        }
        break;
      case 'Escape':
        event.preventDefault();
        this.closeCountryDropdown();
        break;
    }
  }

  private resolveCurrencyForCountry(countryCode: string) {
    this.isResolvingCurrency.set(true);

    this.forexService.getCurrencyForCountry(countryCode).pipe(
      catchError(() => of(null))
    ).subscribe(resolvedCurrency => {
      this.isResolvingCurrency.set(false);
      const supportedList = this.supportedCurrencies().map(c => c.value);

      if (resolvedCurrency && supportedList.includes(resolvedCurrency)) {
        this.needsBaseCurrencySelection.set(false);
        this.registerForm.controls.baseCurrency.clearValidators();
        this.registerForm.controls.baseCurrency.setValue(resolvedCurrency);
      } else {
        this.needsBaseCurrencySelection.set(true);
        this.registerForm.controls.baseCurrency.setValidators([Validators.required]);
        this.registerForm.controls.baseCurrency.setValue('');
      }
      this.registerForm.controls.baseCurrency.updateValueAndValidity();
    });
  }

  toggleCurrencyDropdown(event?: Event) {
    const willOpen = !this.isCurrencyDropdownOpen();
    this.isCurrencyDropdownOpen.set(willOpen);

    if (willOpen) {
      this.isCountryDropdownOpen.set(false);
      const currentVal = this.registerForm.controls.baseCurrency.value;
      const idx = this.supportedCurrencies().findIndex(c => c.value === currentVal);
      this.activeDropdownIndex.set(idx >= 0 ? idx : 0);

      if (event && event.currentTarget) {
        const trigger = event.currentTarget as HTMLElement;
        const modal = trigger.closest('.auth-modal');
        if (modal) {
          const triggerRect = trigger.getBoundingClientRect();
          const modalRect = modal.getBoundingClientRect();
          const spaceBelow = modalRect.bottom - triggerRect.bottom;
          const spaceAbove = triggerRect.top - modalRect.top;
          this.isCurrencyDropdownUp.set(spaceBelow < 240 && spaceAbove > spaceBelow);
        } else {
          const rect = trigger.getBoundingClientRect();
          const spaceBelow = window.innerHeight - rect.bottom;
          this.isCurrencyDropdownUp.set(spaceBelow < 240 && rect.top > 240);
        }
      }
    } else {
      this.registerForm.controls.baseCurrency.markAsTouched();
    }
  }

  closeCurrencyDropdown() {
    this.isCurrencyDropdownOpen.set(false);
    this.registerForm.controls.baseCurrency.markAsTouched();
  }

  selectCurrency(value: string) {
    this.registerForm.controls.baseCurrency.setValue(value);
    this.closeCurrencyDropdown();
  }

  onCurrencyKeydown(event: KeyboardEvent) {
    if (!this.isCurrencyDropdownOpen()) {
      if (event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        this.toggleCurrencyDropdown(event);
      }
      return;
    }

    const maxIdx = this.supportedCurrencies().length - 1;
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.activeDropdownIndex.update(i => Math.min(i + 1, maxIdx));
        this.scrollToActiveOption('.currency-options-list', '.custom-dropdown-option.active');
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.activeDropdownIndex.update(i => Math.max(i - 1, 0));
        this.scrollToActiveOption('.currency-options-list', '.custom-dropdown-option.active');
        break;
      case 'Enter':
        event.preventDefault();
        const active = this.activeDropdownIndex();
        if (active >= 0 && active <= maxIdx) {
          this.selectCurrency(this.supportedCurrencies()[active].value);
        }
        break;
      case 'Escape':
        event.preventDefault();
        this.closeCurrencyDropdown();
        break;
    }
  }

  private scrollToActiveOption(containerSelector: string, activeSelector: string) {
    const container = document.querySelector(containerSelector);
    const activeOption = document.querySelector(activeSelector) as HTMLElement;
    if (container && activeOption) {
      const optionTop = activeOption.offsetTop;
      const optionBottom = optionTop + activeOption.offsetHeight;
      const containerTop = container.scrollTop;
      const containerBottom = containerTop + container.clientHeight;

      if (optionTop < containerTop) {
        container.scrollTop = optionTop;
      } else if (optionBottom > containerBottom) {
        container.scrollTop = optionBottom - container.clientHeight;
      }
    }
  }

  sendOtp() {
    const emailControl = this.registerForm.controls.email;
    if (emailControl.invalid) {
      emailControl.markAsTouched();
      return;
    }

    this.isSendingOtp.set(true);
    this.authService.requestOtp({ email: emailControl.value, purpose: 'REGISTRATION' as any }).subscribe({
      next: () => {
        this.isSendingOtp.set(false);
        this.isOtpSent.set(true);
        this.toast.success('Verification code sent to your email.');

        this.registerForm.controls.otp.enable();
        this.startOtpTimer();
      },
      error: () => this.isSendingOtp.set(false)
    });
  }

  startOtpTimer() {
    this.otpTimerSub?.unsubscribe();
    this.isOtpExpired.set(false);
    this.otpTimeRemaining.set(120);

    const endTime = Date.now() + 120000;

    this.otpTimerSub = timer(0, 1000).subscribe(() => {
      const remaining = Math.max(0, Math.round((endTime - Date.now()) / 1000));
      this.otpTimeRemaining.set(remaining);

      if (remaining <= 0) {
        this.isOtpExpired.set(true);
        this.otpTimerSub?.unsubscribe();
      }
    });
  }

  formatTime(seconds: number): string {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  onOtpInput(event: Event) {
    const input = event.target as HTMLInputElement;
    const sanitized = input.value.replace(/[^0-9]/g, '');
    if (input.value !== sanitized) {
      input.value = sanitized;
      this.registerForm.controls.otp.setValue(sanitized);
    }
  }

  registerSubmit() {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isProcessing.set(true);
    const rawData = this.registerForm.getRawValue();

    const formData: RegisterRequest = {
      fullName: `${rawData.firstName} ${rawData.lastName}`.trim(),
      username: rawData.username,
      email: rawData.email,
      password: rawData.password,
      linkedBankName: rawData.linkedBankName,
      countryCode: rawData.countryCode.toUpperCase(),
      baseCurrency: rawData.baseCurrency ? rawData.baseCurrency.toUpperCase() : '',
      otp: rawData.otp
    };

    this.authService.registerUser(formData).subscribe({
      next: () => {
        this.isProcessing.set(false);
        this.toast.success('Account created successfully. Please log in.');
        this.resetRegisterState();
        this.switchMode(AuthStatus.Login);
      },
      error: () => this.isProcessing.set(false)
    });
  }

  switchMode(mode: AuthStatus) {
    this.currentAuthStatus.set(mode);
    this.loginForm.reset();
    this.resetRegisterState();
  }

  resetRegisterState() {
    this.registerForm.reset();
    this.needsBaseCurrencySelection.set(false);
    this.registerForm.controls.baseCurrency.clearValidators();
    this.registerForm.controls.baseCurrency.setValue('');
    this.registerForm.controls.baseCurrency.updateValueAndValidity();
    this.isOtpSent.set(false);
    this.registerForm.controls.otp.disable();
    this.otpTimerSub?.unsubscribe();
    this.isOtpExpired.set(true);
    this.otpTimeRemaining.set(0);
    this.isCountryDropdownOpen.set(false);
    this.isCurrencyDropdownOpen.set(false);
    this.isCountryDropdownUp.set(false);
    this.isCurrencyDropdownUp.set(false);
    this.isResolvingCurrency.set(false);
  }

  closeAuth() {
    this.resetRegisterState();
    this.showAuth.emit();
  }
}