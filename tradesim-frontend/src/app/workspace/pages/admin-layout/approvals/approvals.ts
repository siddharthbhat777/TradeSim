import { ChangeDetectionStrategy, Component, computed, effect, inject, OnInit, PLATFORM_ID, signal } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { take } from 'rxjs/operators';
import { SegmentedControl, SegmentOption } from '../../../../shared/components/segmented-control/segmented-control';
import { ListingApprovals } from './listing-approvals/listing-approvals';
import { IpoApprovals } from './ipo-approvals/ipo-approvals';
import { WalletApprovals } from './wallet-approvals/wallet-approvals';

export type ApprovalTab = 'Listing' | 'IPO' | 'Wallet';

@Component({
  selector: 'app-approvals',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    SegmentedControl,
    ListingApprovals,
    IpoApprovals,
    WalletApprovals
  ],
  templateUrl: './approvals.html',
  styleUrl: './approvals.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Approvals implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly platformId = inject(PLATFORM_ID);

  readonly isMobile = signal(false);
  readonly highlightedId = signal<string | null>(null);

  readonly tabControl = new FormControl<ApprovalTab>('Listing', { nonNullable: true });

  readonly tabOptions = computed<SegmentOption<ApprovalTab>[]>(() => {
    if (this.isMobile()) {
      return [
        { label: 'Listing', value: 'Listing' },
        { label: 'IPO', value: 'IPO' },
        { label: 'Wallet', value: 'Wallet' }
      ];
    }
    return [
      { label: 'Listing Requests', value: 'Listing' },
      { label: 'IPO Offers', value: 'IPO' },
      { label: 'Wallet Upgrades', value: 'Wallet' }
    ];
  });

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      const mql = window.matchMedia('(max-width: 768px)');
      this.isMobile.set(mql.matches);
      mql.addEventListener('change', (e) => this.isMobile.set(e.matches));
    }
  }

  ngOnInit(): void {
    this.route.queryParams.pipe(take(1)).subscribe(params => {
      let hasParams = false;

      if (params['type']) {
        const type = params['type'] as ApprovalTab;
        if (['Listing', 'IPO', 'Wallet'].includes(type)) {
          this.tabControl.setValue(type);
        }
        hasParams = true;
      }

      if (params['id']) {
        this.highlightedId.set(params['id']);
        hasParams = true;
      }

      if (hasParams) {
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: {},
          replaceUrl: true
        });
      }
    });
  }
}