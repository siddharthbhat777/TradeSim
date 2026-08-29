import { ChangeDetectionStrategy, Component, effect, HostListener, inject, input, model, OnDestroy, output, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';

export type DrawerPosition = 'left' | 'right' | 'top' | 'bottom';

@Component({
  selector: 'app-drawer',
  templateUrl: './drawer.html',
  styleUrl: './drawer.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Drawer implements OnDestroy {
  private readonly document = inject(DOCUMENT);

  readonly isOpen = model<boolean>(false);
  readonly position = input<DrawerPosition>('right');
  readonly closeOnBackdrop = input<boolean>(true);
  readonly hideCloseButton = input<boolean>(false);

  readonly closed = output<void>();

  readonly isVisible = signal<boolean>(false);
  readonly isAnimating = signal<boolean>(false);

  constructor() {
    effect((onCleanup) => {
      const open = this.isOpen();

      if (open) {
        this.document.body.style.overflow = 'hidden';
        this.isVisible.set(true);

        const timer = setTimeout(() => {
          this.isAnimating.set(true);
        }, 20);

        onCleanup(() => clearTimeout(timer));
      } else {
        this.document.body.style.overflow = '';
        this.isAnimating.set(false);

        const timer = setTimeout(() => {
          this.isVisible.set(false);
        }, 300);

        onCleanup(() => clearTimeout(timer));
      }
    }, { allowSignalWrites: true });
  }

  @HostListener('document:keydown.escape')
  onEscape() {
    if (this.isOpen()) {
      this.close();
    }
  }

  ngOnDestroy() {
    this.document.body.style.overflow = '';
  }

  close() {
    this.isOpen.set(false);
    this.closed.emit();
  }

  onBackdropClick(event: MouseEvent) {
    if (this.closeOnBackdrop()) {
      this.close();
    }
  }
}