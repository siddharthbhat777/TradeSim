import { ChangeDetectionStrategy, Component, computed, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'app-slider',
  templateUrl: './slider.html',
  styleUrl: './slider.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Slider),
      multi: true
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Slider implements ControlValueAccessor {
  readonly min = input<number>(0);
  readonly max = input<number>(100);
  readonly step = input<number>(1);
  readonly range = input<boolean>(false);

  readonly value = signal<number | [number, number]>(0);
  protected readonly cvaDisabled = signal(false);

  readonly valA = computed(() => {
    const v = this.value();
    return Array.isArray(v) ? v[0] : this.min();
  });

  readonly valB = computed(() => {
    const v = this.value();
    return Array.isArray(v) ? v[1] : this.max();
  });

  readonly singleVal = computed(() => {
    const v = this.value();
    return Array.isArray(v) ? v[0] : v as number;
  });

  readonly activeMin = computed(() => {
    const v = this.value();
    return Array.isArray(v) ? Math.min(v[0], v[1]) : this.min();
  });

  readonly activeMax = computed(() => {
    const v = this.value();
    return Array.isArray(v) ? Math.max(v[0], v[1]) : v as number;
  });

  readonly percentMin = computed(() => {
    return ((this.activeMin() - this.min()) / (this.max() - this.min())) * 100;
  });

  readonly percentMax = computed(() => {
    return ((this.activeMax() - this.min()) / (this.max() - this.min())) * 100;
  });

  private onChange: (v: number | [number, number]) => void = () => { };
  private onTouched: () => void = () => { };

  writeValue(val: number | [number, number] | null | undefined): void {
    if (val !== undefined && val !== null) {
      this.value.set(val);
    } else {
      this.value.set(this.range() ? [this.min(), this.max()] : this.min());
    }
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.cvaDisabled.set(isDisabled);
  }

  onThumbAChange(event: Event) {
    const val = Number((event.target as HTMLInputElement).value);
    const current = this.value();
    const currentB = Array.isArray(current) ? current[1] : this.max();
    const newVal: [number, number] = [val, currentB];
    this.value.set(newVal);
    this.onChange(newVal);
  }

  onThumbBChange(event: Event) {
    const val = Number((event.target as HTMLInputElement).value);
    const current = this.value();
    const currentA = Array.isArray(current) ? current[0] : this.min();
    const newVal: [number, number] = [currentA, val];
    this.value.set(newVal);
    this.onChange(newVal);
  }

  onSingleChange(event: Event) {
    const val = Number((event.target as HTMLInputElement).value);
    this.value.set(val);
    this.onChange(val);
  }

  onInputBlur() {
    this.onTouched();
  }
}