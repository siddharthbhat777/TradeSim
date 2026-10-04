import { ChangeDetectionStrategy, Component, ElementRef, OnInit, OnDestroy, ViewChild, signal } from '@angular/core';

@Component({
  selector: 'app-features',
  templateUrl: './features.html',
  styleUrl: './features.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Features implements OnInit, OnDestroy {
  @ViewChild('featuresGrid', { static: true }) gridElement!: ElementRef;

  isVisible = signal<boolean>(false);
  private observer: IntersectionObserver | null = null;

  ngOnInit(): void {
    if (typeof window !== 'undefined' && 'IntersectionObserver' in window) {
      this.observer = new IntersectionObserver((entries) => {
        if (entries[0].isIntersecting) {
          this.isVisible.set(true);
          this.observer?.disconnect();
        }
      }, { threshold: 0.15 });

      if (this.gridElement) {
        this.observer.observe(this.gridElement.nativeElement);
      }
    } else {
      this.isVisible.set(true);
    }
  }

  ngOnDestroy(): void {
    if (this.observer) {
      this.observer.disconnect();
    }
  }
}