import { ChangeDetectionStrategy, Component, computed, effect, inject, OnInit, PLATFORM_ID, signal } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { take } from 'rxjs/operators';
import { forkJoin } from 'rxjs';
import { SegmentedControl, SegmentOption } from '../../../../shared/components/segmented-control/segmented-control';
import { ListingApprovals } from './listing-approvals/listing-approvals';
import { IpoApprovals } from './ipo-approvals/ipo-approvals';
import { WalletApprovals } from './wallet-approvals/wallet-approvals';
import { ListingService } from '../../../../services/listing/listing-service';
import { IpoService } from '../../../../services/ipo/ipo-service';
import { WalletService } from '../../../../services/wallet/wallet-service';
import { UserService } from '../../../../services/user/user-service';
import { ListingRequestResponse } from '../../../../models/listing';
import { IpoOfferResponse } from '../../../../models/ipo';
import { Wallet } from '../../../../models/wallet';
import { UserListResponse } from '../../../../models/user';

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

  private readonly listingService = inject(ListingService);
  private readonly ipoService = inject(IpoService);
  private readonly walletService = inject(WalletService);
  private readonly userService = inject(UserService);

  readonly isMobile = signal(false);
  readonly highlightedId = signal<string | null>(null);

  readonly listings = signal<ListingRequestResponse[]>([]);
  readonly ipos = signal<IpoOfferResponse[]>([]);
  readonly wallets = signal<Wallet[]>([]);
  readonly users = signal<UserListResponse[]>([]);
  readonly isLoading = signal(true);

  readonly tabControl = new FormControl<ApprovalTab>('Listing', { nonNullable: true });

  readonly tabOptions = computed<SegmentOption<ApprovalTab>[]>(() => {
    const lCount = this.listings().length;
    const iCount = this.ipos().length;
    const wCount = this.wallets().length;

    if (this.isMobile()) {
      return [
        { label: 'Listing', value: 'Listing', badgeCount: lCount },
        { label: 'IPO', value: 'IPO', badgeCount: iCount },
        { label: 'Wallet', value: 'Wallet', badgeCount: wCount }
      ];
    }
    return [
      { label: 'Listing Requests', value: 'Listing', badgeCount: lCount },
      { label: 'IPO Offers', value: 'IPO', badgeCount: iCount },
      { label: 'Wallet Upgrades', value: 'Wallet', badgeCount: wCount }
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

    this.loadData();
  }

  private loadData(): void {
    this.isLoading.set(true);
    forkJoin({
      listings: this.listingService.getPendingExchangeRequests(),
      ipos: this.ipoService.getPendingIpos(),
      wallets: this.walletService.getPendingMultiCurrencyRequests(),
      users: this.userService.getAllUsers()
    }).subscribe({
      next: ({ listings, ipos, wallets, users }) => {
        listings.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        ipos.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        wallets.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

        this.listings.set(listings);
        this.ipos.set(ipos);
        this.wallets.set(wallets);
        this.users.set(users);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  onListingProcessed(id: string): void {
    this.listings.update(arr => arr.filter(i => i.id !== id));
  }

  onIpoProcessed(id: string): void {
    this.ipos.update(arr => arr.filter(i => i.id !== id));
  }

  onWalletProcessed(id: string): void {
    this.wallets.update(arr => arr.filter(i => i.id !== id));
  }
}