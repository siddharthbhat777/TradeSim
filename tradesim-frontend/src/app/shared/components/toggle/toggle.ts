import { ChangeDetectionStrategy, Component, computed, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export type ToggleSize = 'small' | 'medium' | 'large';
export type ToggleVariant = 'primary' | 'secondary' | 'accent' | 'success' | 'danger' | 'warning';

@Component({
  selector: 'app-toggle',
  templateUrl: './toggle.html',
  styleUrl: './toggle.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Toggle),
      multi: true,
    },
  ],
})
export class Toggle implements ControlValueAccessor {
  private static nextId = 0;

  readonly label = input<string>();
  readonly size = input<ToggleSize>('medium');
  readonly variant = input<ToggleVariant>('primary');
  readonly offVariant = input<ToggleVariant>();

  protected readonly checked = signal(false);
  protected readonly disabled = signal(false);

  protected readonly labelId = `app-toggle-label-${Toggle.nextId++}`;

  protected readonly classes = computed(() => {
    const classList = ['toggle', `toggle--${this.size()}`];

    if (this.checked()) {
      classList.push('toggle--checked', `toggle--color-on-${this.variant()}`);
    } else if (this.offVariant()) {
      classList.push(`toggle--color-off-${this.offVariant()}`);
    }

    if (this.disabled()) {
      classList.push('toggle--disabled');
    }

    return classList.join(' ');
  });

  private onChange: (value: boolean) => void = () => { };
  private onTouched: () => void = () => { };

  protected toggle(): void {
    if (this.disabled()) {
      return;
    }

    const next = !this.checked();
    this.checked.set(next);
    this.onChange(next);
    this.onTouched();
  }

  writeValue(value: boolean): void {
    this.checked.set(!!value);
  }

  registerOnChange(fn: (value: boolean) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }
}