import { ChangeDetectionStrategy, Component, ElementRef, input, signal, viewChild } from '@angular/core';
import { generateUniqueId } from '../../utils/id-generator';

export type TooltipPosition = 'top' | 'right' | 'bottom' | 'left';

const SHOW_DELAY = 300;
const HIDE_DELAY = 100;

@Component({
  selector: 'app-tooltip',
  templateUrl: './tooltip.html',
  styleUrl: './tooltip.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Tooltip {
  private readonly panelRef = viewChild.required<ElementRef<HTMLDivElement>>('panel');

  private readonly uid = generateUniqueId('tt');

  protected readonly tooltipId = `app-tooltip-${this.uid}`;
  protected readonly anchorName = `--app-tooltip-anchor-${this.uid}`;

  readonly text = input('');
  readonly position = input<TooltipPosition>('top');

  protected readonly isOpen = signal(false);

  private showTimeout?: ReturnType<typeof setTimeout>;
  private hideTimeout?: ReturnType<typeof setTimeout>;

  protected onMouseEnter(): void {
    clearTimeout(this.hideTimeout);
    this.showTimeout = setTimeout(() => this.show(), SHOW_DELAY);
  }

  protected onMouseLeave(): void {
    clearTimeout(this.showTimeout);
    this.hideTimeout = setTimeout(() => this.hide(), HIDE_DELAY);
  }

  protected onFocusIn(): void {
    clearTimeout(this.showTimeout);
    clearTimeout(this.hideTimeout);
    this.show();
  }

  protected onFocusOut(): void {
    clearTimeout(this.showTimeout);
    clearTimeout(this.hideTimeout);
    this.hide();
  }

  protected onClick(): void {
    clearTimeout(this.showTimeout);
    clearTimeout(this.hideTimeout);
    this.isOpen() ? this.hide() : this.show();
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && this.isOpen()) {
      this.hide();
    }
  }

  private show(): void {
    if (!this.text()) return;
    const panel = this.panelRef().nativeElement as HTMLElement & { showPopover?: () => void };
    panel.showPopover?.();
    this.isOpen.set(true);
  }

  private hide(): void {
    const panel = this.panelRef().nativeElement as HTMLElement & { hidePopover?: () => void };
    panel.hidePopover?.();
    this.isOpen.set(false);
  }
}