import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastEntry, ToastService } from './toast.service';

@Component({
  selector: 'app-toast',
  templateUrl: './toast.html',
  styleUrl: './toast.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Toast {
  protected readonly toastService = inject(ToastService);

  protected handleAction(entry: ToastEntry): void {
    entry.action?.onClick();
    this.toastService.dismiss(entry.id);
  }
}