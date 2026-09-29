import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { AbstractControl, FormArray, FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';

import { Card } from '../../../../shared/components/card/card';
import { Badge } from '../../../../shared/components/badge/badge';
import { Button } from '../../../../shared/components/button/button';
import { CustomInput } from '../../../../shared/components/input/input';
import { InputDirective } from '../../../../shared/directives/input';
import { Table, TableColumn, TableCellDirective } from '../../../../shared/components/table/table';
import { Dropdown, DropdownOption } from '../../../../shared/components/dropdown/dropdown';
import { SegmentedControl, SegmentOption } from '../../../../shared/components/segmented-control/segmented-control';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { DialogService } from '../../../../shared/components/dialog/dialog.service';

import { CompanyService } from '../../../../services/company/company-service';
import { StockService } from '../../../../services/stock/stock-service';
import { ExchangeService } from '../../../../services/exchange/exchange-service';
import { ListingService } from '../../../../services/listing/listing-service';
import { UserService } from '../../../../services/user/user-service';

import { CompanyResponse, CompanyRepresentativeAssignmentResponse } from '../../../../models/company';
import { Exchange } from '../../../../models/exchange';
import { Stock } from '../../../../models/stock';
import { ListingRequestResponse } from '../../../../models/listing';

export interface OverviewTimelineItem {
  type: 'STOCK' | 'REQUEST';
  id: string;
  symbol: string;
  time: number;
  stock?: Stock;
  request?: ListingRequestResponse;
}

@Component({
  selector: 'app-listing',
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
    Dropdown,
    SegmentedControl
  ],
  templateUrl: './listing.html',
  styleUrl: './listing.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Listing implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly dialog = inject(DialogService);

  private readonly companyService = inject(CompanyService);
  private readonly stockService = inject(StockService);
  private readonly exchangeService = inject(ExchangeService);
  private readonly listingService = inject(ListingService);
  private readonly userService = inject(UserService);

  readonly isLoading = signal(true);
  readonly isApplying = signal(false);
  readonly isSubmitting = signal(false);
  readonly rejectingId = signal<string | null>(null);
  readonly rejectionReasonInput = signal('');

  readonly activeTab = signal<'OVERVIEW' | 'APPROVALS' | 'HISTORY'>('OVERVIEW');
  readonly tabOptions: SegmentOption<'OVERVIEW' | 'APPROVALS' | 'HISTORY'>[] = [
    { label: 'Overview', value: 'OVERVIEW' },
    { label: 'Internal Approvals', value: 'APPROVALS' },
    { label: 'History', value: 'HISTORY' }
  ];

  readonly sectors = signal<string[]>([]);

  readonly companyId = signal<string>('');
  readonly isPrimaryContact = signal(false);

  readonly company = signal<CompanyResponse | null>(null);
  readonly activeStocks = signal<Stock[]>([]);
  readonly exchanges = signal<Exchange[]>([]);
  readonly representatives = signal<CompanyRepresentativeAssignmentResponse[]>([]);
  readonly listingRequests = signal<ListingRequestResponse[]>([]);

  readonly listingForm = this.fb.nonNullable.group({
    symbol: ['', [Validators.required, Validators.maxLength(10), Validators.pattern(/^[A-Z0-9]+$/)]],
    exchangeId: ['', Validators.required],
    referencePrice: [null as unknown as number, [Validators.required, Validators.min(0.01)]],
    sector: ['', Validators.required],
    priceBandPercent: [10, [Validators.required, Validators.min(0.1), Validators.max(50)]],
    totalShares: [null as unknown as number, [Validators.required, Validators.min(1)]],
    capTable: this.fb.array([])
  });

  readonly formValue = toSignal(this.listingForm.valueChanges, { initialValue: this.listingForm.value });

  readonly exchangeOptions = computed<DropdownOption<string>[]>(() =>
    this.exchanges().map(e => ({ label: e.name, value: e.id }))
  );

  readonly sectorOptions = computed<DropdownOption<string>[]>(() =>
    this.sectors().map(s => ({ label: s.replace(/_/g, ' '), value: s }))
  );

  readonly representativeOptions = computed<DropdownOption<string>[]>(() =>
    this.representatives()
      .filter(r => r.status === 'ACTIVE')
      .map(r => ({ label: r.fullName || r.userId, value: r.userId }))
  );

  readonly activeRequests = computed(() => {
    return this.listingRequests().filter(r => r.status !== 'REJECTED' && r.status !== 'APPROVED');
  });

  readonly pendingApprovals = computed(() =>
    this.listingRequests().filter(r => r.status === 'PENDING_INTERNAL_REVIEW')
  );

  readonly overviewTimeline = computed(() => {
    const stocks = this.activeStocks();
    const requests = this.activeRequests();
    const allReqs = this.listingRequests();

    const timeline: OverviewTimelineItem[] = [];

    stocks.forEach(stock => {
      const matchingReq = allReqs.find(r => r.symbol === stock.symbol && r.status === 'APPROVED');
      const time = matchingReq ? new Date(matchingReq.createdAt).getTime() : Date.now();
      timeline.push({
        type: 'STOCK',
        id: `stock-${stock.id}`,
        symbol: stock.symbol,
        time: time,
        stock: stock
      });
    });

    requests.forEach(req => {
      timeline.push({
        type: 'REQUEST',
        id: `req-${req.id}`,
        symbol: req.symbol,
        time: new Date(req.createdAt).getTime(),
        request: req
      });
    });

    return timeline.sort((a, b) => b.time - a.time);
  });

  readonly columns = signal<TableColumn<ListingRequestResponse>[]>([
    { key: 'symbol', header: 'Symbol' },
    { key: 'exchangeName', header: 'Exchange' },
    { key: 'totalShares', header: 'Total Shares' },
    { key: 'status', header: 'Status' },
    { key: 'createdAt', header: 'Date' },
    { key: 'actions', header: '', align: 'right' }
  ]);

  readonly historyColumns = signal<TableColumn<ListingRequestResponse>[]>([
    { key: 'symbol', header: 'Symbol' },
    { key: 'status', header: 'Status' },
    { key: 'rejectionReason', header: 'Notes' },
    { key: 'createdAt', header: 'Date' }
  ]);

  get capTableControls() {
    return (this.listingForm.get('capTable') as FormArray).controls as FormGroup[];
  }

  readonly capTableSum = computed(() => {
    const capTable = this.formValue().capTable || [];
    return capTable.reduce((sum: number, entry: any) => sum + (Number(entry.quantity) || 0), 0);
  });

  readonly remainingShares = computed(() => {
    const total = Number(this.formValue().totalShares) || 0;
    return Math.max(0, total - this.capTableSum());
  });

  constructor() {
    this.listingForm.get('totalShares')?.valueChanges.subscribe(() => this.revalidateCapTable());
    this.listingForm.get('capTable')?.valueChanges.subscribe(() => this.revalidateCapTable());
  }

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
      exchanges: this.exchangeService.getExchanges(),
      reps: this.companyService.getRepresentatives(companyId),
      profile: this.userService.getProfile(),
      requests: this.listingService.getCompanyRequests(companyId),
      stocks: this.stockService.getStocks(),
      sectors: this.stockService.getSectors()
    }).subscribe({
      next: (data) => {
        this.company.set(data.company);
        this.exchanges.set(data.exchanges);
        this.representatives.set(data.reps);
        this.sectors.set(data.sectors);

        const sortedRequests = data.requests.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        this.listingRequests.set(sortedRequests);

        const myRep = data.reps.find(r => r.userId === data.profile.id && r.status === 'ACTIVE');
        this.isPrimaryContact.set(myRep?.assignmentRole === 'PRIMARY_CONTACT');

        const companyStocks = data.stocks.filter(s => s.companyName === data.company.name);
        this.activeStocks.set(companyStocks);

        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  private refreshRequests(): void {
    this.listingService.getCompanyRequests(this.companyId()).subscribe(reqs => {
      const sortedReqs = reqs.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
      this.listingRequests.set(sortedReqs);
    });
  }

  private revalidateCapTable(): void {
    const capTable = this.listingForm.get('capTable') as FormArray;
    capTable.controls.forEach(ctrl => {
      ctrl.get('quantity')?.updateValueAndValidity({ emitEvent: false });
    });
  }

  private readonly capTableQuantityValidator = (control: AbstractControl) => {
    if (!this.listingForm) return null;
    const totalShares = Number(this.listingForm.get('totalShares')?.value) || 0;

    const parentGroup = control.parent;
    if (!parentGroup) return null;

    const formArray = parentGroup.parent as FormArray;
    if (!formArray) return null;

    let otherSum = 0;
    for (const group of formArray.controls) {
      if (group !== parentGroup) {
        otherSum += Number(group.get('quantity')?.value) || 0;
      }
    }

    const maxAllowed = Math.max(0, totalShares - otherSum);

    if (Number(control.value) > maxAllowed) {
      return { maxExceeded: true };
    }

    return null;
  };

  getAvailableRepresentatives(currentIndex: number): DropdownOption<string>[] {
    const capTable = this.formValue().capTable || [];

    const selectedOtherUserIds = new Set(
      capTable
        .map((entry: any, i: number) => i !== currentIndex ? entry.userId : null)
        .filter((id: string | null) => id !== null && id !== '')
    );

    return this.representatives()
      .filter(r => r.status === 'ACTIVE')
      .map(r => {
        const isSelectedElsewhere = selectedOtherUserIds.has(r.userId);
        return {
          label: isSelectedElsewhere ? `${r.fullName || r.userId} (Already Selected)` : (r.fullName || r.userId),
          value: r.userId,
          disabled: isSelectedElsewhere
        };
      });
  }

  addCapTableEntry(): void {
    const capTable = this.listingForm.get('capTable') as FormArray;
    capTable.push(this.fb.nonNullable.group({
      userId: ['', Validators.required],
      quantity: [null as unknown as number, [Validators.required, Validators.min(1), this.capTableQuantityValidator]]
    }));
  }

  removeCapTableEntry(index: number): void {
    const capTable = this.listingForm.get('capTable') as FormArray;
    capTable.removeAt(index);
  }

  submitApplication(): void {
    if (this.listingForm.invalid) {
      this.listingForm.markAllAsTouched();
      return;
    }

    if (this.capTableSum() > Number(this.listingForm.get('totalShares')?.value)) {
      this.toast.danger('Cap table sum cannot exceed total shares.');
      return;
    }

    this.isSubmitting.set(true);

    const payload = this.listingForm.getRawValue() as any;

    this.listingService.submitListingRequest(this.companyId(), payload).subscribe({
      next: () => {
        this.toast.success('Listing application submitted successfully.');
        this.isSubmitting.set(false);
        this.isApplying.set(false);
        this.listingForm.reset({ priceBandPercent: 10 });
        (this.listingForm.get('capTable') as FormArray).clear();
        this.refreshRequests();
      },
      error: () => this.isSubmitting.set(false)
    });
  }

  approveRequest(requestId: string): void {
    this.dialog.open({
      title: 'Approve Listing Request',
      message: 'Are you sure you want to approve this listing request? It will be forwarded to the exchange for final approval.',
      primaryLabel: 'Approve',
      primaryVariant: 'primary',
      secondaryLabel: 'Cancel',
      onPrimary: () => {
        this.listingService.approveInternalRequest(requestId).subscribe(() => {
          this.toast.success('Listing request approved. Forwarded to exchange.');
          this.refreshRequests();
        });
      }
    });
  }

  initiateReject(requestId: string): void {
    this.rejectingId.set(requestId);
    this.rejectionReasonInput.set('');
  }

  cancelReject(): void {
    this.rejectingId.set(null);
  }

  confirmReject(requestId: string): void {
    if (!this.rejectionReasonInput().trim()) {
      this.toast.danger('Please provide a rejection reason.');
      return;
    }

    this.dialog.open({
      title: 'Reject Listing Request',
      message: `Are you sure you want to reject this request with the following reason: "${this.rejectionReasonInput()}"?`,
      primaryLabel: 'Reject',
      primaryVariant: 'danger',
      secondaryLabel: 'Cancel',
      onPrimary: () => {
        this.listingService.rejectInternalRequest(requestId, this.rejectionReasonInput()).subscribe(() => {
          this.toast.success('Listing request rejected.');
          this.rejectingId.set(null);
          this.refreshRequests();
        });
      }
    });
  }

  redraft(request: ListingRequestResponse): void {
    this.listingForm.patchValue({
      symbol: request.symbol,
      exchangeId: request.exchangeId,
      referencePrice: request.referencePrice,
      sector: request.sector,
      priceBandPercent: request.priceBandPercent,
      totalShares: request.totalShares
    });

    const capTable = this.listingForm.get('capTable') as FormArray;
    capTable.clear();
    request.capTable.forEach(entry => {
      capTable.push(this.fb.nonNullable.group({
        userId: [entry.userId, Validators.required],
        quantity: [entry.quantity, [Validators.required, Validators.min(1), this.capTableQuantityValidator]]
      }));
    });

    this.isApplying.set(true);
  }
}