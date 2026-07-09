import {
  booleanAttribute,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  computed,
  inject,
  input,
  signal,
  viewChildren,
} from '@angular/core';
import type { ControlValueAccessor } from '@angular/forms';
import { NgControl } from '@angular/forms';

export interface SegmentOption<T = unknown> {
  label: string;
  value: T;
  disabled?: boolean;
}

export type SegmentedControlSize = 'small' | 'medium' | 'large';

const booleanAttributeOrNull = (value: unknown): boolean | null => {
  if (value === null || value === undefined) {
    return null;
  }

  return booleanAttribute(value);
};

let nextSegmentedId = 0;

@Component({
  selector: 'app-segmented-control',
  templateUrl: './segmented-control.html',
  styleUrl: './segmented-control.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[style.--segment-count]': 'options().length',
    '[style.--active-index]': 'selectedIndex()',
    '[attr.data-size]': 'size()',
    '[class.segmented--full-width]': 'fullWidth()',
    '[class.segmented--disabled]': 'disabledState()'
  }
})
export class SegmentedControl<T = unknown> implements ControlValueAccessor, OnDestroy {
  private readonly ngControl = inject(NgControl, { optional: true, self: true });

  private readonly segmentRefs = viewChildren<ElementRef<HTMLButtonElement>>('segmentButton');

  private readonly uid = nextSegmentedId++;
  protected readonly labelId = `app-segmented-label-${this.uid}`;
  protected readonly errorId = `app-segmented-error-${this.uid}`;

  readonly options = input.required<SegmentOption<T>[]>();
  readonly label = input('');
  readonly ariaLabel = input<string | null>(null);
  readonly size = input<SegmentedControlSize>('medium');
  readonly fullWidth = input(false, { transform: booleanAttribute });
  readonly required = input(false, { transform: booleanAttribute });
  readonly errorMessage = input('Please select an option.');
  readonly reserveMessageSpace = input<boolean | null>(null, {
    transform: booleanAttributeOrNull
  });

  protected readonly value = signal<T | null>(null);
  protected readonly disabledState = signal(false);
  protected readonly localTouched = signal(false);

  private cvaInitialized = false;
  protected readonly deselectable = signal(false);

  protected readonly justSelected = signal(false);
  private popTimeout?: ReturnType<typeof setTimeout>;

  protected readonly selectedIndex = computed(() =>
    this.options().findIndex((option) => option.value === this.value())
  );

  protected readonly focusIndex = computed(() => {
    const selected = this.selectedIndex();

    if (selected !== -1) {
      return selected;
    }

    return this.options().findIndex((option) => !option.disabled);
  });

  protected readonly showError = computed(
    () => this.required() && this.localTouched() && this.selectedIndex() === -1
  );

  protected readonly shouldReserveMessageSpace = computed(() => {
    if (this.reserveMessageSpace() !== null) {
      return this.reserveMessageSpace();
    }

    return this.required();
  });

  private onChange: (value: T | null) => void = () => { };
  private onTouched: () => void = () => { };

  constructor() {
    if (this.ngControl) {
      this.ngControl.valueAccessor = this;
    }
  }

  ngOnDestroy(): void {
    clearTimeout(this.popTimeout);
  }

  protected selectIndex(index: number): void {
    const option = this.options()[index];

    if (!option || option.disabled || this.disabledState()) {
      return;
    }

    const currentIndex = this.selectedIndex();

    if (index === currentIndex) {
      if (this.deselectable()) {
        this.commitValue(null);
      }
      this.markTouched();
      return;
    }

    this.commitValue(option.value, currentIndex === -1);
    this.markTouched();
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (this.disabledState()) {
      return;
    }

    const enabled = this.enabledIndices();

    if (!enabled.length) {
      return;
    }

    let target: number;

    switch (event.key) {
      case 'ArrowRight':
      case 'ArrowDown':
        event.preventDefault();
        target = this.stepEnabled(enabled, 1);
        break;
      case 'ArrowLeft':
      case 'ArrowUp':
        event.preventDefault();
        target = this.stepEnabled(enabled, -1);
        break;
      case 'Home':
        event.preventDefault();
        target = enabled[0];
        break;
      case 'End':
        event.preventDefault();
        target = enabled[enabled.length - 1];
        break;
      default:
        return;
    }

    this.selectIndex(target);
    this.segmentRefs()[target]?.nativeElement.focus();
  }

  protected markTouched(): void {
    if (!this.localTouched()) {
      this.localTouched.set(true);
    }
    this.onTouched();
  }

  markAsTouched(): void {
    this.markTouched();
  }

  private commitValue(value: T | null, isFirstSelection = false): void {
    if (isFirstSelection && value !== null) {
      this.triggerPop();
    }

    this.value.set(value);
    this.onChange(value);
  }

  private triggerPop(): void {
    this.justSelected.set(true);
    clearTimeout(this.popTimeout);
    this.popTimeout = setTimeout(() => this.justSelected.set(false), 220);
  }

  private enabledIndices(): number[] {
    return this.options().reduce<number[]>((acc, option, index) => {
      if (!option.disabled) {
        acc.push(index);
      }
      return acc;
    }, []);
  }

  private stepEnabled(enabled: number[], delta: number): number {
    const currentPos = enabled.indexOf(this.focusIndex());
    const basePos = currentPos === -1 ? 0 : currentPos;
    const nextPos = (basePos + delta + enabled.length) % enabled.length;
    return enabled[nextPos];
  }

  writeValue(value: T | null | undefined): void {
    if (!this.cvaInitialized) {
      this.cvaInitialized = true;
      this.deselectable.set(value === null || value === undefined);
    }
    this.value.set(value ?? null);
  }

  registerOnChange(fn: (value: T | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabledState.set(isDisabled);
  }
}