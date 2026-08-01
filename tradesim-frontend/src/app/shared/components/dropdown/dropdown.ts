import {
  booleanAttribute,
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  DOCUMENT,
  ElementRef,
  effect,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import type { ControlValueAccessor } from '@angular/forms';
import { NgControl } from '@angular/forms';
import { CustomInput } from '../input/input';
import { InputDirective } from '../../directives/input';
import { InlineLoader } from '../loaders/inline-loader/inline-loader';
import { generateUniqueId } from '../../utils/id-generator';

export interface DropdownOption<T = unknown> {
  label: string;
  value: T;
  icon?: string;
  disabled?: boolean;
}

export type DropdownSize = 'small' | 'medium' | 'large';

const booleanAttributeOrNull = (value: unknown): boolean | null => {
  if (value === null || value === undefined) {
    return null;
  }
  return booleanAttribute(value);
};

@Component({
  selector: 'app-dropdown',
  imports: [CustomInput, InputDirective, InlineLoader],
  templateUrl: './dropdown.html',
  styleUrl: './dropdown.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Dropdown<T = unknown> implements ControlValueAccessor {
  private readonly ngControl = inject(NgControl, { optional: true, self: true });
  private readonly hostRef = inject(ElementRef);
  private readonly searchInputRef = viewChild<ElementRef<HTMLInputElement>>('searchInput');

  private readonly uid = generateUniqueId('dd');

  protected readonly triggerId = `app-dropdown-trigger-${this.uid}`;
  protected readonly labelId = `app-dropdown-label-${this.uid}`;

  readonly options = input.required<DropdownOption<T>[]>();
  readonly label = input('');
  readonly placeholder = input('');
  readonly helperText = input('');
  readonly errorText = input('');
  readonly size = input<DropdownSize>('large');
  readonly required = input(false, { transform: booleanAttribute });
  readonly disabled = input(false, { transform: booleanAttribute });
  readonly fullWidth = input(false, { transform: booleanAttribute });
  readonly showErrors = input(true, { transform: booleanAttribute });
  readonly searchable = input(false, { transform: booleanAttribute });
  readonly searchPlaceholder = input('Search...');
  readonly emptyText = input('No options available');
  readonly loading = input(false, { transform: booleanAttribute });
  readonly panelLoading = input(false, { transform: booleanAttribute });
  readonly reserveMessageSpace = input<boolean | null>(null, {
    transform: booleanAttributeOrNull,
  });

  readonly opened = output<void>();

  protected readonly selected = signal<DropdownOption<T> | null>(null);
  protected readonly isOpen = signal(false);
  protected readonly openUpwards = signal(false);
  protected readonly activeIndex = signal(-1);
  protected readonly cvaDisabled = signal(false);
  protected readonly searchQuery = signal('');

  protected readonly disabledState = computed(() => this.disabled() || this.cvaDisabled());

  protected readonly filteredOptions = computed(() => {
    if (!this.searchable()) {
      return this.options();
    }
    const query = this.searchQuery().trim().toLowerCase();
    if (!query) {
      return this.options();
    }
    return this.options().filter((option) => option.label.toLowerCase().includes(query));
  });

  protected readonly activeOptionId = computed(() => {
    const index = this.activeIndex();
    return index >= 0 && index < this.filteredOptions().length ? this.optionId(index) : null;
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

  private readonly window = inject(DOCUMENT).defaultView;
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    if (this.ngControl) {
      this.ngControl.valueAccessor = this;
    }

    this.destroyRef.onDestroy(() => {
      this.window?.removeEventListener('click', this.onWindowClick, { capture: true });
      this.window?.removeEventListener('scroll', this.calculatePosition, { capture: true });
      this.window?.removeEventListener('resize', this.calculatePosition);
    });

    effect(() => {
      const opts = this.options();
      if (!this.placeholder() && this.selected() === null && opts.length > 0) {
        const first = opts.find((option) => !option.disabled) ?? opts[0];
        this.selected.set(first);
        this.onChange(first.value);
      }
    });

    effect(() => {
      if (this.isOpen()) {
        this.calculatePosition();
        this.window?.addEventListener('click', this.onWindowClick, { capture: true });
        this.window?.addEventListener('scroll', this.calculatePosition, { capture: true, passive: true });
        this.window?.addEventListener('resize', this.calculatePosition, { passive: true });
      } else {
        this.window?.removeEventListener('click', this.onWindowClick, { capture: true });
        this.window?.removeEventListener('scroll', this.calculatePosition, { capture: true });
        this.window?.removeEventListener('resize', this.calculatePosition);
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

  private calculatePosition = (): void => {
    if (!this.isOpen()) {
      return;
    }
    const rect = this.hostRef.nativeElement.getBoundingClientRect();
    const viewportHeight = this.window?.innerHeight || 0;
    const spaceBelow = viewportHeight - rect.bottom;
    this.openUpwards.set(spaceBelow < 320 && rect.top > spaceBelow);
  };

  private onWindowClick = (event: MouseEvent): void => {
    const target = event.target as Node | null;
    if (target && !this.hostRef.nativeElement.contains(target)) {
      this.isOpen.set(false);
      this.onTouched();
    }
  };

  protected togglePanel(): void {
    if (this.disabledState() || this.loading()) {
      return;
    }
    const willOpen = !this.isOpen();
    this.isOpen.set(willOpen);

    if (willOpen) {
      this.searchQuery.set('');
      this.activeIndex.set(this.computeInitialActiveIndex());
      this.opened.emit();
      if (this.searchable()) {
        queueMicrotask(() => this.searchInputRef()?.nativeElement.focus());
      }
    } else {
      this.onTouched();
    }
  }

  protected selectOption(option: DropdownOption<T>): void {
    if (option.disabled || this.disabledState()) {
      return;
    }
    if (this.placeholder() && this.selected() === option) {
      this.selected.set(null);
      this.onChange(null);
    } else {
      this.selected.set(option);
      this.onChange(option.value);
    }
    this.onTouched();
    this.isOpen.set(false);
  }

  protected onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchQuery.set(value);
    this.activeIndex.set(this.firstEnabledIndex());
  }

  protected onSearchKeydown(event: KeyboardEvent): void {
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.moveActive(1);
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.moveActive(-1);
        break;
      case 'Home':
        event.preventDefault();
        this.activeIndex.set(this.firstEnabledIndex());
        break;
      case 'End':
        event.preventDefault();
        this.activeIndex.set(this.lastEnabledIndex());
        break;
      case 'Enter':
        event.preventDefault();
        this.commitActive();
        break;
    }
  }

  protected onTriggerKeydown(event: KeyboardEvent): void {
    if (this.disabledState() || this.loading()) {
      return;
    }
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.isOpen() ? this.moveActive(1) : this.togglePanel();
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.isOpen() ? this.moveActive(-1) : this.togglePanel();
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
        this.isOpen() ? this.commitActive() : this.togglePanel();
        break;
      default:
        if (this.isOpen() && !this.searchable() && event.key.length === 1) {
          this.handleTypeAhead(event.key);
        }
    }
  }

  private enabledIndices(): number[] {
    return this.filteredOptions().reduce<number[]>((acc, option, index) => {
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
      const index = this.filteredOptions().indexOf(current);
      if (index !== -1) {
        return index;
      }
    }
    return this.firstEnabledIndex();
  }

  private commitActive(): void {
    const option = this.filteredOptions()[this.activeIndex()];
    if (option && !option.disabled) {
      this.selectOption(option);
    }
  }

  private handleTypeAhead(char: string): void {
    clearTimeout(this.typeAheadTimeout);
    this.typeAheadBuffer += char.toLowerCase();
    const options = this.filteredOptions();
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
    this.cvaDisabled.set(isDisabled);
  }
}