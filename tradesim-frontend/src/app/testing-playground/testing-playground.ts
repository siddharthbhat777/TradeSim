import { Component, inject, signal } from '@angular/core';
import { Button } from '../shared/components/button/button';
import { Badge } from '../shared/components/badge/badge';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CustomInput } from '../shared/components/input/input';
import { InputDirective } from '../shared/directives/input';
import { CardBorder, CardComponent, CardVariant } from '../shared/components/card/card';
import { Dialog } from '../shared/components/dialog/dialog';
import { PriceIndicator } from '../shared/components/price-indicator/price-indicator';
import { EmptyState } from '../shared/components/empty-state/empty-state';
import { Router } from '@angular/router';
import { Toggle } from '../shared/components/toggle/toggle';
import { Dropdown, DropdownOption } from '../shared/components/dropdown/dropdown';
import { Tooltip } from '../shared/components/tooltip/tooltip';
import { ToastService } from '../shared/components/toast/toast.service';
import { Toast } from '../shared/components/toast/toast';

@Component({
  selector: 'app-testing-playground',
  imports: [Button, Badge, ReactiveFormsModule, InputDirective, CustomInput, CardComponent, Dialog, PriceIndicator, EmptyState, Toggle, Dropdown, Tooltip, Toast],
  templateUrl: './testing-playground.html',
  styleUrl: './testing-playground.scss'
})
export class TestingPlayground {
  readonly router = inject(Router);

  readonly inputDemoForm = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email]
    }),
    search: new FormControl('', {
      nonNullable: true
    }),
    notes: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(10)]
    })
  });

  protected readonly variants: CardVariant[] = [
    'default',
    'surface',
    'outline',
    'accent',
    'success',
    'danger',
    'warning',
  ];

  protected readonly borders: CardBorder[] = [
    'primary',
    'secondary',
    'accent',
    'success',
    'danger',
    'warning',
  ];

  protected readonly clickCount = signal(0);

  protected readonly handleCardClick = () => {
    this.clickCount.update((count) => count + 1);
  };

  protected readonly isBasicDialogOpen = signal(false);
  protected readonly isBodyOnlyDialogOpen = signal(false);
  protected readonly isHeaderOnlyDialogOpen = signal(false);
  protected readonly isForcedDialogOpen = signal(false);
  protected readonly isCustomContentDialogOpen = signal(false);


  readonly darkModeControl = new FormControl(false, { nonNullable: true });

  readonly settingsForm = new FormGroup({
    emailAlerts: new FormControl(true, { nonNullable: true }),
    smsAlerts: new FormControl(false, { nonNullable: true }),
    autoInvest: new FormControl(false, { nonNullable: true }),
  });

  readonly lockedFeatureControl = new FormControl({ value: true, disabled: true });

  readonly riskModeControl = new FormControl(false, { nonNullable: true });
  riskModeLocked = false;

  toggleLock(): void {
    this.riskModeLocked = !this.riskModeLocked;
    if (this.riskModeLocked) {
      this.riskModeControl.disable();
    } else {
      this.riskModeControl.enable();
    }
  }

  get settingsSummary(): string {
    const v = this.settingsForm.value;
    return `emailAlerts: ${v.emailAlerts}, smsAlerts: ${v.smsAlerts}, autoInvest: ${v.autoInvest}`;
  }

  readonly orderTypeControl = new FormControl<string | null>(null);
  readonly orderTypeOptions: DropdownOption<string>[] = [
    { label: 'Market', value: 'market' },
    { label: 'Limit', value: 'limit' },
    { label: 'Stop', value: 'stop', disabled: true },
  ];

  readonly sortByControl = new FormControl<string | null>(null);
  readonly sortByOptions: DropdownOption<string>[] = [
    { label: 'Price', value: 'price' },
    { label: 'Name', value: 'name' },
    { label: 'Change %', value: 'change' },
  ];

  readonly tradePrefsForm = new FormGroup({
    exchange: new FormControl<string | null>(null, { validators: [Validators.required] }),
  });
  readonly exchangeOptions: DropdownOption<string>[] = [
    { label: 'NSE', value: 'nse' },
    { label: 'BSE', value: 'bse' },
    { label: 'NASDAQ', value: 'nasdaq' },
  ];

  protected submitTradePrefs(): void {
    this.tradePrefsForm.markAllAsTouched();
  }

  readonly currencyControl = new FormControl<string | null>(null);
  readonly currencyOptions: DropdownOption<string>[] = [
    { label: 'Indian Rupee', value: 'inr', icon: '🇮🇳' },
    { label: 'US Dollar', value: 'usd', icon: '🇺🇸' },
    { label: 'Euro', value: 'eur', icon: '🇪🇺' },
  ];

  readonly lockedExchangeControl = new FormControl<string | null>({ value: 'nse', disabled: true });

  readonly runtimeLockControl = new FormControl<string | null>(null);
  runtimeLockControlLocked = false;
  toggleDropdownLock(): void {
    this.runtimeLockControlLocked = !this.runtimeLockControlLocked;
    this.runtimeLockControlLocked ? this.runtimeLockControl.disable() : this.runtimeLockControl.enable();
  }

  readonly watchlistCategoryControl = new FormControl<string | null>(null, { validators: [Validators.required] });
  readonly watchlistCategoryOptions: DropdownOption<string>[] = [
    { label: 'Technology', value: 'tech' },
    { label: 'Banking', value: 'banking' },
    { label: 'Energy', value: 'energy' },
    { label: 'Pharma', value: 'pharma' },
  ];

  readonly toastService = inject(ToastService);

  protected fireStackTest(): void {
    for (let i = 1; i <= 6; i++) {
      this.toastService.info(`Queued toast #${i}`);
    }
  }

  protected fireUndoDemo(): void {
    this.toastService.danger('Manager access revoked', {
      action: {
        label: 'Undo',
        onClick: () => {
          console.log('Re-adding manager to the managers table...');
          this.toastService.success('Manager access restored');
        },
      },
    });
  }

  protected fireReorderDemo(): void {
    this.toastService.info('Order cancelled', {
      action: {
        label: 'Reorder',
        onClick: () => {
          console.log('Opening prefilled order form for the cancelled order...');
          this.toastService.success('Reorder form opened');
        },
      }
    });
  }

  protected fireToastPosition(): void {
    this.toastService.info('Toast position bottom', {
      position: 'bottom-center'
    });
  }
}
