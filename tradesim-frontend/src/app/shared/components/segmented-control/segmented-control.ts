import { ChangeDetectionStrategy, Component, computed, ElementRef, inject, input, signal, viewChildren } from '@angular/core';
import { ControlValueAccessor, NgControl } from '@angular/forms';
import { generateUniqueId } from '../../utils/id-generator';
import { booleanAttribute } from '@angular/core';

export interface SegmentOption<T = unknown> {
  label: string;
  value: T;
  disabled?: boolean;
}

@Component({
  selector: 'app-segmented-control',
  templateUrl: './segmented-control.html',
  styleUrl: './segmented-control.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[attr.data-size]': 'size()',
    '[class.full-width]': 'fullWidth()'
  }
})
export class SegmentedControl<T = unknown> implements ControlValueAccessor {
  private readonly ngControl = inject(NgControl, { optional: true, self: true });
  private readonly uid = generateUniqueId('seg');

  readonly options = input.required<SegmentOption<T>[]>();
  readonly label = input<string>('');
  readonly size = input<'small' | 'medium' | 'large'>('medium');
  readonly fullWidth = input(false, { transform: booleanAttribute });
  readonly required = input(false, { transform: booleanAttribute });
  readonly ariaLabel = input<string>('Segmented Control');
  readonly errorMessage = input<string>('Please select an option.');

  readonly labelId = `seg-label-${this.uid}`;
  readonly errorId = `seg-error-${this.uid}`;

  protected readonly selectedIndex = signal<number>(-1);
  protected readonly focusIndex = signal<number>(0);
  protected readonly cvaDisabled = signal(false);
  protected readonly touched = signal(false);
  protected readonly justSelected = signal(false);

  private onChange: (value: T | null) => void = () => { };
  private onTouched: () => void = () => { };

  readonly segmentButtons = viewChildren<ElementRef<HTMLButtonElement>>('segmentButton');

  protected readonly disabledState = computed(() => this.cvaDisabled());

  protected readonly showError = computed(() => {
    if (!this.required()) return false;
    const control = this.ngControl?.control;
    if (control) {
      return control.invalid && (control.touched || control.dirty);
    }
    return this.touched() && this.selectedIndex() === -1;
  });

  protected readonly shouldReserveMessageSpace = computed(() => {
    const control = this.ngControl?.control;
    return this.required() || (control?.validator || control?.asyncValidator);
  });

  constructor() {
    if (this.ngControl) {
      this.ngControl.valueAccessor = this;
    }
  }

  writeValue(value: T | null | undefined): void {
    if (value === null || value === undefined) {
      this.selectedIndex.set(-1);
      return;
    }
    const index = this.options().findIndex(opt => opt.value === value);
    this.selectedIndex.set(index);
    if (index !== -1) {
      this.focusIndex.set(index);
    }
  }

  registerOnChange(fn: (value: T | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.cvaDisabled.set(isDisabled);
  }

  selectIndex(index: number) {
    if (this.disabledState() || this.options()[index].disabled) return;

    if (this.selectedIndex() === index) {
      if (this.required()) return;
      this.selectedIndex.set(-1);
      this.onChange(null);
    } else {
      this.selectedIndex.set(index);
      this.onChange(this.options()[index].value);
    }
    this.markTouched();
    this.focusIndex.set(index);

    this.justSelected.set(true);
    setTimeout(() => this.justSelected.set(false), 300);
  }

  markTouched() {
    if (!this.touched()) {
      this.touched.set(true);
      this.onTouched();
    }
  }

  onKeydown(event: KeyboardEvent) {
    if (this.disabledState()) return;

    const options = this.options();
    let newIndex = this.focusIndex();

    switch (event.key) {
      case 'ArrowRight':
      case 'ArrowDown':
        event.preventDefault();
        do {
          newIndex = (newIndex + 1) % options.length;
        } while (options[newIndex].disabled);
        break;
      case 'ArrowLeft':
      case 'ArrowUp':
        event.preventDefault();
        do {
          newIndex = (newIndex - 1 + options.length) % options.length;
        } while (options[newIndex].disabled);
        break;
      case 'Home':
        event.preventDefault();
        newIndex = options.findIndex(o => !o.disabled);
        break;
      case 'End':
        event.preventDefault();
        for (let i = options.length - 1; i >= 0; i--) {
          if (!options[i].disabled) {
            newIndex = i;
            break;
          }
        }
        break;
      case 'Enter':
      case ' ':
        event.preventDefault();
        this.selectIndex(newIndex);
        return;
      default:
        return;
    }

    this.focusIndex.set(newIndex);
    this.focusButton(newIndex);
  }

  private focusButton(index: number) {
    const buttons = this.segmentButtons();
    if (buttons && buttons[index]) {
      buttons[index].nativeElement.focus();
    }
  }
}