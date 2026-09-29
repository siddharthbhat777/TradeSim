import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';

import { Card } from '../../../../shared/components/card/card';
import { Badge } from '../../../../shared/components/badge/badge';
import { Button } from '../../../../shared/components/button/button';
import { CustomInput } from '../../../../shared/components/input/input';
import { InputDirective } from '../../../../shared/directives/input';
import { Table, TableColumn, TableCellDirective, TableExpandedRowDirective } from '../../../../shared/components/table/table';
import { Dropdown, DropdownOption } from '../../../../shared/components/dropdown/dropdown';
import { SegmentedControl, SegmentOption } from '../../../../shared/components/segmented-control/segmented-control';
import { ToastService } from '../../../../shared/components/toast/toast.service';

import { CompanyService } from '../../../../services/company/company-service';
import { StockService } from '../../../../services/stock/stock-service';
import { IpoService } from '../../../../services/ipo/ipo-service';
import { UserService } from '../../../../services/user/user-service';

import { CompanyResponse, CompanyRepresentativeAssignmentResponse } from '../../../../models/company';
import { Stock } from '../../../../models/stock';
import { IpoOfferResponse, IpoSubscriptionResponse } from '../../../../models/ipo';

@Component({
  selector: 'app-ipo',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    Card,
    Badge,
    Button,
    CustomInput,
    InputDirective,
    Table,
    TableCellDirective,
    TableExpandedRowDirective,
    Dropdown,
    SegmentedControl
  ],
  templateUrl: './ipo.html',
  styleUrl: './ipo.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Ipo implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);

  private readonly companyService = inject(CompanyService);
  private readonly stockService = inject(StockService);
  private readonly ipoService = inject(IpoService);
  private readonly userService = inject(UserService);

  readonly isLoading = signal(true);
  readonly isApplying = signal(false);
  readonly isSubmitting = signal(false);

  readonly activeTab = signal<'OVERVIEW' | 'SUBSCRIPTIONS' | 'HISTORY'>('OVERVIEW');
  readonly tabOptions: SegmentOption<'OVERVIEW' | 'SUBSCRIPTIONS' | 'HISTORY'>[] = [
    { label: 'Overview', value: 'OVERVIEW' },
    { label: 'Subscriptions', value: 'SUBSCRIPTIONS' },
    { label: 'History', value: 'HISTORY' }
  ];

  readonly companyId = signal<string>('');
  readonly isPrimaryContact = signal(false);

  readonly company = signal<CompanyResponse | null>(null);
  readonly activeStocks = signal<Stock[]>([]);
  readonly ipoOffers = signal<IpoOfferResponse[]>([]);
  readonly subscriptions = signal<IpoSubscriptionResponse[]>([]);

  readonly ipoForm = this.fb.nonNullable.group({
    stockId: ['', Validators.required],
    issuePrice: [null as unknown as number, [Validators.required, Validators.min(0.01)]],
    sharesPerAllottee: [null as unknown as number, [Validators.required, Validators.min(1)]],
    maxAllottees: [null as unknown as number, [Validators.required, Validators.min(1)]],
    subscriptionStartAt: ['', Validators.required],
    subscriptionEndAt: ['', Validators.required]
  });

  readonly formValue = toSignal(this.ipoForm.valueChanges, { initialValue: this.ipoForm.value });

  readonly eligibleStocks = computed(() => {
    const offers = this.ipoOffers();
    return this.activeStocks().filter(stock =>
      stock.status === 'HALTED' &&
      !offers.some(o => o.stockId === stock.id && (o.status === 'PENDING_APPROVAL' || o.status === 'SUBSCRIPTION_OPEN' || o.status === 'ALLOTTED'))
    );
  });

  readonly stockOptions = computed<DropdownOption<string>[]>(() =>
    this.eligibleStocks().map(s => ({ label: s.symbol, value: s.id }))
  );

  readonly activeIpo = computed(() => {
    return this.ipoOffers().find(o => o.status === 'PENDING_APPROVAL' || o.status === 'SUBSCRIPTION_OPEN') || null;
  });

  readonly historyIpos = computed(() => {
    return this.ipoOffers().filter(o => o.status === 'REJECTED' || o.status === 'ALLOTTED');
  });

  readonly totalSharesOffered = computed(() => {
    const f = this.formValue();
    const lotSize = Number(f.sharesPerAllottee) || 0;
    const max = Number(f.maxAllottees) || 0;
    return lotSize * max;
  });

  readonly subscriptionColumns = signal<TableColumn<IpoSubscriptionResponse>[]>([
    { key: 'user', header: 'Subscriber' },
    { key: 'symbol', header: 'IPO Symbol' },
    { key: 'lockedAmount', header: 'Locked Amount', align: 'right' },
    { key: 'status', header: 'Status' },
    { key: 'createdAt', header: 'Date', align: 'right' }
  ]);

  readonly historyColumns = signal<TableColumn<IpoOfferResponse>[]>([
    { key: 'symbol', header: 'Symbol' },
    { key: 'issuePrice', header: 'Price', align: 'right' },
    { key: 'totalSharesOffered', header: 'Offered', align: 'right' },
    { key: 'status', header: 'Status' },
    { key: 'rejectionReason', header: 'Notes' },
    { key: 'createdAt', header: 'Date' }
  ]);

  ngOnInit(): void {
    const id = this.route.parent?.snapshot.paramMap.get('companyId') || this.route.snapshot.paramMap.get('companyId');
    if (id) {
      this.companyId.set(id);
      this.loadDashboardData(id);
    }
  }

  private loadDashboardData(companyId: string): void {
    this.isLoading.set(true);

    forkJoin({
      company: this.companyService.getCompany(companyId),
      reps: this.companyService.getRepresentatives(companyId),
      profile: this.userService.getProfile(),
      stocks: this.stockService.getStocks(),
      offers: this.ipoService.getCompanyIpoOffers(companyId)
    }).subscribe({
      next: (data) => {
        this.company.set(data.company);

        const sortedOffers = data.offers.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        this.ipoOffers.set(sortedOffers);

        const myRep = data.reps.find(r => r.userId === data.profile.id && r.status === 'ACTIVE');
        this.isPrimaryContact.set(myRep?.assignmentRole === 'PRIMARY_CONTACT');

        const companyStocks = data.stocks.filter(s => s.companyName === data.company.name);
        this.activeStocks.set(companyStocks);

        this.isLoading.set(false);
        this.checkAndLoadSubscriptions(sortedOffers);
      },
      error: () => this.isLoading.set(false)
    });
  }

  private checkAndLoadSubscriptions(offers: IpoOfferResponse[]): void {
    const relevantOffers = offers.filter(o => o.status !== 'PENDING_APPROVAL' && o.status !== 'REJECTED');
    if (relevantOffers.length === 0) {
      this.subscriptions.set([]);
      return;
    }

    const requests = relevantOffers.map(o => this.ipoService.getSubscriptionsForOffer(o.id));
    forkJoin(requests).subscribe({
      next: (results) => {
        const allSubs = results.flat().sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        this.subscriptions.set(allSubs);
      }
    });
  }

  private refreshOffers(): void {
    this.ipoService.getCompanyIpoOffers(this.companyId()).subscribe(offers => {
      const sortedOffers = offers.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
      this.ipoOffers.set(sortedOffers);
      this.checkAndLoadSubscriptions(sortedOffers);
    });
  }

  copyId(id: string): void {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(id).then(() => {
        this.toast.success('Subscriber ID copied to clipboard');
      });
    }
  }

  submitApplication(): void {
    if (this.ipoForm.invalid) {
      this.ipoForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    const f = this.ipoForm.getRawValue();
    const payload = {
      issuePrice: f.issuePrice,
      sharesPerAllottee: f.sharesPerAllottee,
      maxAllottees: f.maxAllottees,
      subscriptionStartAt: new Date(f.subscriptionStartAt).toISOString(),
      subscriptionEndAt: new Date(f.subscriptionEndAt).toISOString()
    };

    this.ipoService.submitIpoOffer(this.companyId(), f.stockId, payload).subscribe({
      next: () => {
        this.toast.success('IPO configuration submitted successfully.');
        this.isSubmitting.set(false);
        this.isApplying.set(false);
        this.ipoForm.reset();
        this.refreshOffers();
      },
      error: () => this.isSubmitting.set(false)
    });
  }

  redraft(offer: IpoOfferResponse): void {
    const stockFound = this.activeStocks().find(s => s.id === offer.stockId);
    if (!stockFound || stockFound.status !== 'HALTED') {
      this.toast.danger('The associated stock is no longer eligible for an IPO.');
      return;
    }

    const formatToLocalDatetime = (isoStr: string) => {
      const date = new Date(isoStr);
      return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
    };

    this.ipoForm.patchValue({
      stockId: offer.stockId,
      issuePrice: offer.issuePrice,
      sharesPerAllottee: offer.sharesPerAllottee,
      maxAllottees: offer.maxAllottees,
      subscriptionStartAt: formatToLocalDatetime(offer.subscriptionStartAt),
      subscriptionEndAt: formatToLocalDatetime(offer.subscriptionEndAt)
    });

    this.isApplying.set(true);
  }
}