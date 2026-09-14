import { ChangeDetectionStrategy, Component, computed, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SegmentedControl } from '../../../shared/components/segmented-control/segmented-control';
import { OngoingIpos } from './ongoing-ipos/ongoing-ipos';
import { AppliedIpos } from './applied-ipos/applied-ipos';
import { UpcomingIpos } from './upcoming-ipos/upcoming-ipos';

export type IpoTab = 'ONGOING' | 'APPLIED' | 'UPCOMING';

@Component({
  selector: 'app-ipo',
  imports: [
    CommonModule,
    FormsModule,
    SegmentedControl,
    OngoingIpos,
    AppliedIpos,
    UpcomingIpos
  ],
  templateUrl: './ipo.html',
  styleUrl: './ipo.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Ipo implements OnDestroy {
  readonly activeTab = signal<IpoTab>('ONGOING');
  readonly isMobile = signal<boolean>(false);

  private mediaQueryList: MediaQueryList;
  private mediaQueryListener: (e: MediaQueryListEvent) => void;

  constructor() {
    this.mediaQueryList = window.matchMedia('(max-width: 768px)');
    this.isMobile.set(this.mediaQueryList.matches);

    this.mediaQueryListener = (e: MediaQueryListEvent) => {
      this.isMobile.set(e.matches);
    };
    this.mediaQueryList.addEventListener('change', this.mediaQueryListener);
  }

  ngOnDestroy(): void {
    this.mediaQueryList.removeEventListener('change', this.mediaQueryListener);
  }

  readonly tabOptions = computed(() => {
    const mobile = this.isMobile();
    return [
      { label: mobile ? 'Ongoing' : 'Ongoing IPOs', value: 'ONGOING' },
      { label: mobile ? 'Applied' : 'My Applications', value: 'APPLIED' },
      { label: mobile ? 'Upcoming' : 'Upcoming IPOs', value: 'UPCOMING' }
    ];
  });
}