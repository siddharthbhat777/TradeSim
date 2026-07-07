import {
  Component,
  ElementRef,
  DestroyRef,
  ChangeDetectionStrategy,
  input,
  output,
  viewChild,
  afterNextRender,
  inject,
} from '@angular/core';
import { DOCUMENT } from '@angular/common';

@Component({
  selector: 'app-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:keydown.escape)': 'handleEscape()',
  },
  templateUrl: './modal.html',
  styleUrl: './modal.scss',
})
export class Modal {
  readonly closeOnBackdropClick = input<boolean>(true);
  readonly closeOnEscape = input<boolean>(true);
  readonly ariaLabel = input<string>('Dialog');
  readonly titleId = input<string | undefined>(undefined);
  readonly closed = output<void>();

  private readonly modalContentRef = viewChild<ElementRef<HTMLElement>>('modalContent');
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);
  private readonly window = this.document.defaultView;
  private previouslyFocusedElement: HTMLElement | null = null;
  private originalBodyOverflow = '';
  private originalBodyPaddingRight = '';

  constructor() {
    afterNextRender(() => {
      this.previouslyFocusedElement = this.document.activeElement as HTMLElement;
      this.lockScroll();
      const content = this.modalContentRef()?.nativeElement;
      if (content) {
        const focusable = this.getFocusableElements(content);
        (focusable[0] ?? content).focus();
      }
    });

    this.destroyRef.onDestroy(() => {
      this.unlockScroll();
      this.previouslyFocusedElement?.focus();
    });
  }

  private lockScroll(): void {
    if (!this.window) {
      return;
    }

    const body = this.document.body;
    this.originalBodyOverflow = body.style.overflow;
    this.originalBodyPaddingRight = body.style.paddingRight;

    const scrollbarWidth = this.window.innerWidth - this.document.documentElement.clientWidth;
    const currentPaddingRight = parseFloat(this.window.getComputedStyle(body).paddingRight) || 0;

    body.style.overflow = 'hidden';
    if (scrollbarWidth > 0) {
      body.style.paddingRight = `${currentPaddingRight + scrollbarWidth}px`;
    }
  }

  private unlockScroll(): void {
    this.document.body.style.overflow = this.originalBodyOverflow;
    this.document.body.style.paddingRight = this.originalBodyPaddingRight;
  }

  protected handleBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget && this.closeOnBackdropClick()) {
      this.requestClose();
    }
  }

  protected handleEscape(): void {
    if (this.closeOnEscape()) {
      this.requestClose();
    }
  }

  protected requestClose(): void {
    this.closed.emit();
  }

  protected trapFocus(event: Event): void {
    const keyboardEvent = event as KeyboardEvent;
    const content = this.modalContentRef()?.nativeElement;
    if (!content) {
      return;
    }

    const focusable = this.getFocusableElements(content);
    if (focusable.length === 0) {
      keyboardEvent.preventDefault();
      return;
    }

    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    const active = this.document.activeElement;

    if (keyboardEvent.shiftKey && active === first) {
      keyboardEvent.preventDefault();
      last.focus();
    } else if (!keyboardEvent.shiftKey && active === last) {
      keyboardEvent.preventDefault();
      first.focus();
    }
  }

  private getFocusableElements(container: HTMLElement): HTMLElement[] {
    const selector =
      'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])';
    return Array.from(container.querySelectorAll<HTMLElement>(selector));
  }
}