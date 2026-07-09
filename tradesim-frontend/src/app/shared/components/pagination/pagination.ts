import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  input,
  model,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Dropdown, type DropdownOption } from '../dropdown/dropdown';
import { CustomInput } from '../input/input';
import { InputDirective } from '../../directives/input';
import { Button } from '../button/button';

export type PaginationSize = 'small' | 'medium' | 'large';

@Component({
  selector: 'app-pagination',
  imports: [FormsModule, Dropdown, CustomInput, InputDirective, Button],
  templateUrl: './pagination.html',
  styleUrl: './pagination.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Pagination {
  totalItems = input.required<number>();
  pageSizeOptions = input<number[]>([10, 25, 50, 100]);
  size = input<PaginationSize>('medium');

  currentPage = model<number>(1);
  pageSize = model<number>(10);

  protected totalPages = computed(() =>
    Math.max(1, Math.ceil(this.totalItems() / this.pageSize()))
  );

  protected isFirstPage = computed(() => this.currentPage() <= 1);
  protected isLastPage = computed(() => this.currentPage() >= this.totalPages());

  protected rangeStart = computed(() =>
    this.totalItems() === 0 ? 0 : (this.currentPage() - 1) * this.pageSize() + 1
  );

  protected rangeEnd = computed(() =>
    Math.min(this.currentPage() * this.pageSize(), this.totalItems()),
  );

  protected navBtnDimension = computed(() => {
    switch (this.size()) {
      case 'small':
        return '2.375rem';
      case 'large':
        return '2.875rem';
      default:
        return '2.625rem';
    }
  });

  protected pageSizeDropdownOptions = computed<DropdownOption<number>[]>(() =>
    this.pageSizeOptions().map((size) => ({ label: `${size} per page`, value: size })),
  );

  protected pageInputValue = signal<string>('1');
  protected isShaking = signal(false);

  private shakeTimeout?: ReturnType<typeof setTimeout>;

  constructor() {
    effect(() => {
      const page = this.currentPage();
      this.pageInputValue.set(String(page));
    });

    effect(() => {
      const total = this.totalPages();
      if (this.currentPage() > total) {
        this.currentPage.set(total);
      }
    });
  }

  protected goToPrevious(): void {
    if (!this.isFirstPage()) {
      this.currentPage.set(this.currentPage() - 1);
    }
  }

  protected goToNext(): void {
    if (!this.isLastPage()) {
      this.currentPage.set(this.currentPage() + 1);
    }
  }

  protected onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(1);
  }

  protected onInput(event: Event): void {
    const inputEl = event.target as HTMLInputElement;
    const value = inputEl.value;

    if (this.isValidPartialValue(value)) {
      this.pageInputValue.set(value);
      return;
    }

    inputEl.value = this.pageInputValue();
    this.triggerShake();
  }

  protected onEnter(): void {
    const value = this.pageInputValue();

    if (value === '') {
      this.pageInputValue.set(String(this.currentPage()));
      return;
    }

    this.currentPage.set(Number(value));
  }

  protected onBlur(): void {
    this.pageInputValue.set(String(this.currentPage()));
  }

  private isValidPartialValue(value: string): boolean {
    if (value === '') {
      return true;
    }

    if (!/^[1-9][0-9]*$/.test(value)) {
      return false;
    }

    const numericValue = Number(value);
    return numericValue >= 1 && numericValue <= this.totalPages();
  }

  private triggerShake(): void {
    this.isShaking.set(false);
    requestAnimationFrame(() => {
      this.isShaking.set(true);
      clearTimeout(this.shakeTimeout);
      this.shakeTimeout = setTimeout(() => this.isShaking.set(false), 300);
    });
  }
}