import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Card } from '../../../../../shared/components/card/card';
import { Dropdown, DropdownOption } from '../../../../../shared/components/dropdown/dropdown';
import { Button } from '../../../../../shared/components/button/button';
import { Modal } from '../../../../../shared/components/modal/modal';
import { CustomInput } from '../../../../../shared/components/input/input';
import { InputDirective } from '../../../../../shared/directives/input';
import { ToastService } from '../../../../../shared/components/toast/toast.service';
import { ExchangeService } from '../../../../../services/exchange/exchange-service';
import { ForexService } from '../../../../../services/forex/forex-service';
import { Exchange, ExchangeMarketClock } from '../../../../../models/exchange';

@Component({
  selector: 'app-exchange-slate',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    Card,
    Dropdown,
    Button,
    Modal,
    CustomInput,
    InputDirective
  ],
  templateUrl: './exchange-slate.html',
  styleUrl: './exchange-slate.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExchangeSlate {
  exchanges = input.required<Exchange[]>();
  selectedExchangeId = input.required<string | null>();
  clocks = input.required<Map<string, ExchangeMarketClock>>();
  stocksCount = input.required<number>();
  indicesCount = input.required<number>();

  exchangeChange = output<string | null>();
  exchangeCreated = output<Exchange>();

  private readonly fb = inject(FormBuilder);
  private readonly exchangeService = inject(ExchangeService);
  private readonly forexService = inject(ForexService);
  private readonly toastService = inject(ToastService);

  readonly showCreateModal = signal(false);
  readonly isCreating = signal(false);
  readonly isLoadingDefaults = signal(false);

  readonly dynamicCurrencyOptions = signal<DropdownOption<string>[]>([]);
  readonly dynamicTimezoneOptions = signal<DropdownOption<string>[]>([]);

  readonly createForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    code: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9]+$/)]],
    countryCode: ['', Validators.required],
    currency: ['', Validators.required],
    timezone: ['', Validators.required],
    marketOpenTime: ['09:15:00', [Validators.required, Validators.pattern(/^([01]\d|2[0-3]):([0-5]\d):([0-5]\d)$/)]],
    marketCloseTime: ['15:30:00', [Validators.required, Validators.pattern(/^([01]\d|2[0-3]):([0-5]\d):([0-5]\d)$/)]]
  });

  readonly exchangeOptions = computed<DropdownOption<string>[]>(() => {
    return this.exchanges().map(e => ({ label: `${e.name} (${e.code})`, value: e.id }));
  });

  readonly countryOptions = computed<DropdownOption<string>[]>(() => {
    return this.forexService.countries().map(c => ({
      label: `${c.name} (${c.code})`,
      value: c.code
    }));
  });

  readonly currentExchange = computed(() => {
    const id = this.selectedExchangeId();
    return this.exchanges().find(e => e.id === id) ?? null;
  });

  readonly currentExchangeClock = computed(() => {
    const id = this.selectedExchangeId();
    if (!id) return null;
    return this.clocks().get(id) ?? null;
  });

  constructor() {
    this.createForm.controls.countryCode.valueChanges.pipe(takeUntilDestroyed()).subscribe(countryCode => {
      if (this.showCreateModal()) {
        if (countryCode) {
          this.fetchCountryDefaults(countryCode);
        } else {
          this.dynamicCurrencyOptions.set([]);
          this.dynamicTimezoneOptions.set([]);
          this.createForm.patchValue({ currency: '', timezone: '' });
          this.createForm.controls.currency.enable();
          this.createForm.controls.timezone.enable();
        }
      }
    });
  }

  onExchangeSelect(val: string | null): void {
    this.exchangeChange.emit(val);
  }

  openCreateModal(): void {
    this.createForm.enable();
    this.createForm.reset({
      name: '',
      code: '',
      countryCode: '',
      currency: '',
      timezone: '',
      marketOpenTime: '09:15:00',
      marketCloseTime: '15:30:00'
    });
    this.dynamicCurrencyOptions.set([]);
    this.dynamicTimezoneOptions.set([]);
    this.showCreateModal.set(true);
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
  }

  private fetchCountryDefaults(countryCode: string): void {
    this.isLoadingDefaults.set(true);
    this.createForm.patchValue({ currency: '', timezone: '' });
    this.createForm.controls.currency.enable();
    this.createForm.controls.timezone.enable();

    this.forexService.getCurrencyForCountry(countryCode).subscribe({
      next: (res) => {
        let parsed: any;
        try {
          parsed = typeof res === 'string' ? JSON.parse(res) : res;
        } catch (e) {
          parsed = { currency: res, timezones: ['UTC'] };
        }

        const curOptions = parsed.currency
          ? [{ label: parsed.currency, value: parsed.currency }]
          : [];

        const tzOptions = (parsed.timezones || []).map((t: string) => ({ label: t, value: t }));

        this.dynamicCurrencyOptions.set(curOptions);
        this.dynamicTimezoneOptions.set(tzOptions);

        const patch: any = {};

        if (curOptions.length > 0) {
          patch.currency = curOptions[0].value;
          if (curOptions.length === 1) {
            this.createForm.controls.currency.disable();
          }
        }

        if (tzOptions.length > 0) {
          patch.timezone = tzOptions[0].value;
          if (tzOptions.length === 1) {
            this.createForm.controls.timezone.disable();
          }
        }

        if (Object.keys(patch).length > 0) {
          this.createForm.patchValue(patch);
        }

        this.isLoadingDefaults.set(false);
      },
      error: () => {
        this.dynamicCurrencyOptions.set([]);
        this.dynamicTimezoneOptions.set([]);
        this.isLoadingDefaults.set(false);
        this.toastService.danger('Failed to fetch country defaults.');
      }
    });
  }

  onTimeInput(event: Event, controlName: 'marketOpenTime' | 'marketCloseTime'): void {
    const input = event.target as HTMLInputElement;
    let val = input.value.replace(/[^0-9]/g, '');

    if (val.length > 6) {
      val = val.substring(0, 6);
    }

    let formatted = '';
    for (let i = 0; i < val.length; i++) {
      if (i === 2 || i === 4) {
        formatted += ':';
      }
      formatted += val[i];
    }

    this.createForm.controls[controlName].setValue(formatted);
    input.value = formatted;
  }

  submitCreate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.isCreating.set(true);
    const payload = this.createForm.getRawValue();

    payload.code = payload.code.toUpperCase();
    payload.countryCode = payload.countryCode.toUpperCase();
    payload.currency = payload.currency.toUpperCase();

    this.exchangeService.createExchange(payload).subscribe({
      next: (newEx) => {
        this.exchangeCreated.emit(newEx);
        this.isCreating.set(false);
        this.showCreateModal.set(false);
        this.toastService.success('Exchange created successfully.');
      },
      error: () => {
        this.isCreating.set(false);
      }
    });
  }
}