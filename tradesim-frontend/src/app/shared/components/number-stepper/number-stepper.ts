import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';

export type StepperVariant = 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'outline';
export type StepperSize = 'small' | 'medium' | 'large';

@Component({
  selector: 'app-number-stepper',
  templateUrl: './number-stepper.html',
  styleUrl: './number-stepper.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NumberStepper {
  readonly value = model<number>(0);
  readonly min = input<number>(0);
  readonly max = input<number>(Number.MAX_SAFE_INTEGER);
  readonly size = input<StepperSize>('medium');
  readonly variant = input<StepperVariant>('primary');
  readonly secondaryVariant = input<StepperVariant | null>(null);

  readonly rightVariant = computed(() => this.secondaryVariant() ?? this.variant());

  decrement(): void {
    const current = this.value();
    if (current > this.min()) {
      this.value.set(current - 1);
    }
  }

  increment(): void {
    const current = this.value();
    if (current < this.max()) {
      this.value.set(current + 1);
    }
  }

  onInputChange(event: Event): void {
    const inputElement = event.target as HTMLInputElement;
    let newValue = parseInt(inputElement.value, 10);

    if (isNaN(newValue)) {
      inputElement.value = String(this.value());
      return;
    }

    if (newValue < this.min()) {
      newValue = this.min();
    } else if (newValue > this.max()) {
      newValue = this.max();
    }

    this.value.set(newValue);
    inputElement.value = String(newValue);
  }
}