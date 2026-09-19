import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Card } from '../../../../../shared/components/card/card';
import { Badge } from '../../../../../shared/components/badge/badge';
import { Button } from '../../../../../shared/components/button/button';
import { CustomInput } from '../../../../../shared/components/input/input';
import { InputDirective } from '../../../../../shared/directives/input';
import { FormatCurrencyPipe } from '../../../../../shared/pipes/format-currency-pipe';
import { Dropdown, DropdownOption } from '../../../../../shared/components/dropdown/dropdown';
import { EmptyState } from '../../../../../shared/components/empty-state/empty-state';
import { ForexService } from '../../../../../services/forex/forex-service';
import { ToastService } from '../../../../../shared/components/toast/toast.service';

@Component({
  selector: 'app-forex-and-fees',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    Card,
    Badge,
    Button,
    CustomInput,
    InputDirective,
    FormatCurrencyPipe,
    Dropdown,
    EmptyState
  ],
  templateUrl: './forex-and-fees.html',
  styleUrl: './forex-and-fees.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ForexAndFees {
  currencies = input.required<string[]>();

  private readonly forexService = inject(ForexService);
  private readonly toastService = inject(ToastService);

  readonly sourceCurrency = new FormControl<string>('USD', { nonNullable: true });
  readonly targetCurrency = new FormControl<string>('INR', { nonNullable: true });
  readonly amountToConvert = new FormControl<number | null>(null);

  readonly convertedRate = signal<number | null>(null);
  readonly isCalculating = signal(false);

  readonly currencyOptions = computed<DropdownOption<string>[]>(() => {
    return this.currencies().map(c => ({ label: c, value: c }));
  });

  calculateRate(): void {
    if (this.sourceCurrency.invalid || this.targetCurrency.invalid || this.amountToConvert.invalid || !this.amountToConvert.value) {
      return;
    }

    this.isCalculating.set(true);
    this.forexService.getExchangeRate(this.sourceCurrency.value, this.targetCurrency.value).subscribe({
      next: (rate) => {
        this.convertedRate.set(rate * this.amountToConvert.value!);
        this.isCalculating.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.convertedRate.set(null);
        this.isCalculating.set(false);
        this.toastService.danger(err.error?.message ?? 'Something went wrong.');
      }
    });
  }

  swapCurrencies(): void {
    const temp = this.sourceCurrency.value;
    this.sourceCurrency.setValue(this.targetCurrency.value);
    this.targetCurrency.setValue(temp);
    this.convertedRate.set(null);
  }
}