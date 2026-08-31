import { ChangeDetectionStrategy, Component, effect, inject, input, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ExchangeService } from '../../../../services/exchange/exchange-service';
import { Badge } from '../../../../shared/components/badge/badge';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-clock',
  imports: [CommonModule, Badge],
  templateUrl: './clock.html',
  styleUrl: './clock.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Clock implements OnDestroy {
  exchangeId = input<string | null>(null);

  private exchangeService = inject(ExchangeService);

  currentTime = signal<Date | null>(null);
  timezone = signal<string>('');
  isOpen = signal<boolean>(false);
  isLoaded = signal<boolean>(false);

  private timer: ReturnType<typeof setInterval> | null = null;
  private apiSub?: Subscription;

  constructor() {
    effect(() => {
      const id = this.exchangeId();
      this.cleanup();
      this.isLoaded.set(false);

      if (id) {
        this.apiSub = this.exchangeService.getMarketClock(id).subscribe({
          next: (res) => {
            this.currentTime.set(new Date(res.currentInstant));
            this.timezone.set(res.timezone);
            this.isOpen.set(res.marketOpenNow);
            this.isLoaded.set(true);

            this.timer = setInterval(() => {
              this.currentTime.update(d => (d ? new Date(d.getTime() + 1000) : null));
            }, 1000);
          },
          error: () => {
            this.isLoaded.set(false);
          }
        });
      }
    });
  }

  ngOnDestroy(): void {
    this.cleanup();
  }

  private cleanup(): void {
    if (this.timer) {
      clearInterval(this.timer);
    }
    if (this.apiSub) {
      this.apiSub.unsubscribe();
    }
  }
}