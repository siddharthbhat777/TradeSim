import {
  booleanAttribute,
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import type { ControlValueAccessor } from '@angular/forms';
import { NgControl } from '@angular/forms';

export interface DropdownOption<T = unknown> {
  label: string;
  value: T;
  icon?: string;
  disabled?: boolean;
}

const booleanAttributeOrNull = (value: unknown): boolean | null => {
  if (value === null || value === undefined) {
    return null;
  }

  return booleanAttribute(value);
};

let nextDropdownId = 0;

@Component({
  selector: 'app-dropdown',
  templateUrl: './dropdown.html',
  styleUrl: './dropdown.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dropdown<T = unknown> implements ControlValueAccessor {
  private readonly ngControl = inject(NgControl, { optional: true, self: true });

  private readonly panelRef = viewChild.required<ElementRef<HTMLUListElement>>('panel');

  private readonly uid = nextDropdownId++;

  protected readonly triggerId = `app-dropdown-trigger-${this.uid}`;
  protected readonly panelId = `app-dropdown-panel-${this.uid}`;
  protected readonly labelId = `app-dropdown-label-${this.uid}`;
  protected readonly anchorName = `--app-dropdown-anchor-${this.uid}`;

  readonly options = input.required<DropdownOption<T>[]>();
  readonly label = input('');
  readonly placeholder = input('');
  readonly helperText = input('');
  readonly errorText = input('');
  readonly required = input(false, { transform: booleanAttribute });
  readonly fullWidth = input(false, { transform: booleanAttribute });
  readonly showErrors = input(true, { transform: booleanAttribute });
  readonly reserveMessageSpace = input<boolean | null>(null, {
    transform: booleanAttributeOrNull,
  });

  protected readonly selected = signal<DropdownOption<T> | null>(null);
  protected readonly isOpen = signal(false);
  protected readonly activeIndex = signal(-1);
  protected readonly disabledState = signal(false);

  protected readonly activeOptionId = computed(() => {
    const index = this.activeIndex();
    return index >= 0 && index < this.options().length ? this.optionId(index) : null;
  });

  protected readonly shouldReserveMessageSpace = computed(() => {
    if (this.reserveMessageSpace() !== null) {
      return this.reserveMessageSpace();
    }

    const control = this.ngControl?.control;

    return !!(
      this.errorText() ||
      this.helperText() ||
      this.required() ||
      control?.validator ||
      control?.asyncValidator
    );
  });

  private onChange: (value: T | null) => void = () => { };
  private onTouched: () => void = () => { };

  private typeAheadBuffer = '';
  private typeAheadTimeout?: ReturnType<typeof setTimeout>;

  constructor() {
    if (this.ngControl) {
      this.ngControl.valueAccessor = this;
    }

    effect(() => {
      const opts = this.options();

      if (!this.placeholder() && this.selected() === null && opts.length > 0) {
        const first = opts.find((option) => !option.disabled) ?? opts[0];
        this.selected.set(first);
        this.onChange(first.value);
      }
    });
  }

  protected optionId(index: number): string {
    return `app-dropdown-option-${this.uid}-${index}`;
  }

  protected shouldShowError(): boolean {
    if (!this.showErrors()) {
      return false;
    }

    const control = this.ngControl?.control;

    if (!control || control.disabled || !control.invalid) {
      return false;
    }

    return control.touched || control.dirty;
  }

  protected getErrorMessage(): string {
    if (this.errorText()) {
      return this.errorText();
    }

    const errors = this.ngControl?.control?.errors;

    if (errors?.['required']) {
      return 'This field is required';
    }

    return 'Invalid selection';
  }

  protected openPanel(): void {
    const panel = this.panelRef().nativeElement as HTMLElement & { showPopover?: () => void };
    panel.showPopover?.();
  }

  protected closePanel(): void {
    const panel = this.panelRef().nativeElement as HTMLElement & { hidePopover?: () => void };
    panel.hidePopover?.();
  }

  protected onPanelToggle(event: Event): void {
    const toggleEvent = event as Event & { newState: 'open' | 'closed' };
    const open = toggleEvent.newState === 'open';
    this.isOpen.set(open);

    if (open) {
      this.activeIndex.set(this.computeInitialActiveIndex());
    } else {
      this.onTouched();
    }
  }

  protected selectOption(option: DropdownOption<T>): void {
    if (option.disabled || this.disabledState()) {
      return;
    }

    this.selected.set(option);
    this.onChange(option.value);
    this.onTouched();
    this.closePanel();
  }

  protected onTriggerKeydown(event: KeyboardEvent): void {
    if (this.disabledState()) {
      return;
    }

    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.isOpen() ? this.moveActive(1) : this.openPanel();
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.isOpen() ? this.moveActive(-1) : this.openPanel();
        break;
      case 'Home':
        if (this.isOpen()) {
          event.preventDefault();
          this.activeIndex.set(this.firstEnabledIndex());
        }
        break;
      case 'End':
        if (this.isOpen()) {
          event.preventDefault();
          this.activeIndex.set(this.lastEnabledIndex());
        }
        break;
      case 'Enter':
      case ' ':
        event.preventDefault();
        this.isOpen() ? this.commitActive() : this.openPanel();
        break;
      default:
        if (this.isOpen() && event.key.length === 1) {
          this.handleTypeAhead(event.key);
        }
    }
  }

  private enabledIndices(): number[] {
    return this.options().reduce<number[]>((acc, option, index) => {
      if (!option.disabled) {
        acc.push(index);
      }
      return acc;
    }, []);
  }

  private firstEnabledIndex(): number {
    return this.enabledIndices()[0] ?? -1;
  }

  private lastEnabledIndex(): number {
    const indices = this.enabledIndices();
    return indices[indices.length - 1] ?? -1;
  }

  private moveActive(delta: number): void {
    const indices = this.enabledIndices();

    if (!indices.length) {
      return;
    }

    const currentPos = indices.indexOf(this.activeIndex());
    const nextPos = Math.min(Math.max(currentPos + delta, 0), indices.length - 1);
    this.activeIndex.set(indices[nextPos === -1 ? 0 : nextPos]);
  }

  private computeInitialActiveIndex(): number {
    const current = this.selected();

    if (current) {
      const index = this.options().indexOf(current);
      if (index !== -1) {
        return index;
      }
    }

    return this.firstEnabledIndex();
  }

  private commitActive(): void {
    const option = this.options()[this.activeIndex()];

    if (option && !option.disabled) {
      this.selectOption(option);
    }
  }

  private handleTypeAhead(char: string): void {
    clearTimeout(this.typeAheadTimeout);
    this.typeAheadBuffer += char.toLowerCase();

    const options = this.options();
    const startFrom = this.activeIndex() + 1;
    const ordered = [...options.slice(startFrom), ...options.slice(0, startFrom)];
    const match = ordered.find(
      (option) => !option.disabled && option.label.toLowerCase().startsWith(this.typeAheadBuffer),
    );

    if (match) {
      this.activeIndex.set(options.indexOf(match));
    }

    this.typeAheadTimeout = setTimeout(() => {
      this.typeAheadBuffer = '';
    }, 600);
  }

  writeValue(value: T | null | undefined): void {
    if (value === null || value === undefined) {
      this.selected.set(null);
      return;
    }

    this.selected.set(this.options().find((option) => option.value === value) ?? null);
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