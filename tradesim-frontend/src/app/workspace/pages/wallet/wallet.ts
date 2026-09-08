import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WalletService } from '../../../services/wallet/wallet-service';
import { TradingAccountService } from '../../../services/trading-account/trading-account-service';
import { LedgerService } from '../../../services/ledger/ledger-service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { LedgerEntryResponse, LedgerEntryType } from '../../../models/ledger';
import { Card } from '../../../shared/components/card/card';
import { Button } from '../../../shared/components/button/button';
import { Badge, BadgeColor } from '../../../shared/components/badge/badge';
import { Table, TableColumn, TableCellDirective, TableExpandedRowDirective } from '../../../shared/components/table/table';
import { Drawer } from '../../../shared/components/drawer/drawer';
import { CustomInput } from '../../../shared/components/input/input';
import { InputDirective } from '../../../shared/directives/input';
import { CheckboxGroup } from '../../../shared/components/checkbox/checkbox-group/checkbox-group';
import { FundManager } from '../../components/fund-manager/fund-manager';
import { SegmentedControl } from '../../../shared/components/segmented-control/segmented-control';
import { Slider } from '../../../shared/components/slider/slider';

@Component({
  selector: 'app-wallet',
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    Card,
    Button,
    Badge,
    Table,
    TableCellDirective,
    TableExpandedRowDirective,
    Drawer,
    CustomInput,
    InputDirective,
    CheckboxGroup,
    FundManager,
    SegmentedControl,
    Slider
  ],
  templateUrl: './wallet.html',
  styleUrl: './wallet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Wallet implements OnInit {
  private readonly walletService = inject(WalletService);
  private readonly tradingAccountService = inject(TradingAccountService);
  private readonly ledgerService = inject(LedgerService);
  private readonly toastService = inject(ToastService);

  readonly wallet = computed(() => this.walletService.wallet());
  readonly baseCurrency = computed(() => this.tradingAccountService.tradingAccount()?.baseCurrency || 'INR');

  readonly baseBucket = computed(() => {
    return this.wallet()?.buckets.find(b => b.currency === this.baseCurrency()) ?? null;
  });

  readonly foreignBuckets = computed(() => {
    return this.wallet()?.buckets.filter(b => b.currency !== this.baseCurrency()) ?? [];
  });

  readonly isMultiCurrencyApproved = computed(() => this.wallet()?.multiCurrencyStatus === 'APPROVED');
  readonly multiCurrencyStatus = computed(() => this.wallet()?.multiCurrencyStatus ?? 'UNREQUESTED');

  readonly ledgerData = signal<LedgerEntryResponse[]>([]);
  readonly isLedgerLoading = signal<boolean>(true);
  readonly isRequestingAccess = signal<boolean>(false);

  readonly filterSearch = signal<string>('');

  readonly isFilterDrawerOpen = signal<boolean>(false);

  readonly activeFilterTypes = signal<string[]>([]);
  readonly pendingFilterTypes = signal<string[]>([]);

  readonly activeDirection = signal<string>('ALL');
  readonly pendingDirection = signal<string>('ALL');

  readonly activeStartDate = signal<string>('');
  readonly pendingStartDate = signal<string>('');

  readonly activeEndDate = signal<string>('');
  readonly pendingEndDate = signal<string>('');

  readonly amountRangeBounds = computed(() => {
    const data = this.ledgerData();
    if (data.length === 0) return { min: 0, max: 10000 };
    const amounts = data.map(d => d.amount);
    return {
      min: Math.floor(Math.min(...amounts)),
      max: Math.ceil(Math.max(...amounts))
    };
  });

  readonly amountSliderStep = computed(() => {
    return Math.max(1, Math.floor(this.amountRangeBounds().max / 100));
  });

  readonly _appliedAmountRange = signal<[number, number] | null>(null);
  readonly _draftAmountRange = signal<[number, number] | null>(null);

  readonly appliedAmountRange = computed(() => {
    const val = this._appliedAmountRange();
    return val ? val : [this.amountRangeBounds().min, this.amountRangeBounds().max] as [number, number];
  });

  readonly draftAmountRange = computed(() => {
    const val = this._draftAmountRange();
    return val ? val : [this.amountRangeBounds().min, this.amountRangeBounds().max] as [number, number];
  });

  readonly filterTypeOptions = [
    { label: 'Deposits & Withdrawals', value: 'TRANSFER' },
    { label: 'Trading & Margin', value: 'TRADE' },
    { label: 'IPO Actions', value: 'IPO' },
    { label: 'Fees', value: 'FEE' }
  ];

  readonly directionOptions = [
    { label: 'All', value: 'ALL' },
    { label: 'Credit (+)', value: 'CREDIT' },
    { label: 'Debit (-)', value: 'DEBIT' }
  ];

  readonly tableCurrentPage = signal<number>(1);
  readonly tablePageSize = signal<number>(20);

  readonly tableColumns = signal<TableColumn<LedgerEntryResponse>[]>([
    { key: 'createdAt', header: 'Date', width: '100px' },
    { key: 'description', header: 'Description' },
    { key: 'amount', header: 'Amount', align: 'right', width: '180px' }
  ]);

  readonly filteredLedger = computed(() => {
    let data = this.ledgerData();
    const query = this.filterSearch().toLowerCase().trim();
    const types = this.activeFilterTypes();
    const direction = this.activeDirection();
    const start = this.activeStartDate();
    const end = this.activeEndDate();

    const amtRange = this.appliedAmountRange();
    const minAmt = Math.min(amtRange[0], amtRange[1]);
    const maxAmt = Math.max(amtRange[0], amtRange[1]);
    const bounds = this.amountRangeBounds();

    if (query) {
      data = data.filter(entry =>
        entry.description.toLowerCase().includes(query) ||
        entry.currency.toLowerCase().includes(query)
      );
    }

    if (direction !== 'ALL') {
      data = data.filter(entry => {
        const isDebit = entry.type.includes('DEBIT') || entry.type.includes('LOCK') || entry.type.includes('FEE') || entry.type === 'WITHDRAWAL';
        return direction === 'DEBIT' ? isDebit : !isDebit;
      });
    }

    if (start) {
      const startDate = new Date(start + 'T00:00:00').getTime();
      data = data.filter(entry => new Date(entry.createdAt).getTime() >= startDate);
    }

    if (end) {
      const endDate = new Date(end + 'T23:59:59.999').getTime();
      data = data.filter(entry => new Date(entry.createdAt).getTime() <= endDate);
    }

    if (minAmt > bounds.min || maxAmt < bounds.max) {
      data = data.filter(entry => entry.amount >= minAmt && entry.amount <= maxAmt);
    }

    if (types.length > 0) {
      data = data.filter(entry => {
        if (types.includes('TRANSFER') && (entry.type === 'DEPOSIT' || entry.type === 'WITHDRAWAL')) return true;
        if (types.includes('TRADE') && (entry.type.includes('TRADE') || entry.type.includes('MARGIN'))) return true;
        if (types.includes('IPO') && entry.type.includes('IPO')) return true;
        if (types.includes('FEE') && entry.type.includes('FEE')) return true;
        return false;
      });
    }

    return data;
  });

  readonly hasActiveFilters = computed(() => {
    const amtRange = this.appliedAmountRange();
    const minAmt = Math.min(amtRange[0], amtRange[1]);
    const maxAmt = Math.max(amtRange[0], amtRange[1]);
    const bounds = this.amountRangeBounds();

    return this.activeFilterTypes().length > 0 ||
      this.activeDirection() !== 'ALL' ||
      this.activeStartDate() !== '' ||
      this.activeEndDate() !== '' ||
      minAmt > bounds.min ||
      maxAmt < bounds.max;
  });

  readonly hasFilterChanges = computed(() => {
    if (this.activeDirection() !== this.pendingDirection()) return true;
    if (this.activeStartDate() !== this.pendingStartDate()) return true;
    if (this.activeEndDate() !== this.pendingEndDate()) return true;

    const activeRange = this.appliedAmountRange();
    const draftRange = this.draftAmountRange();
    if (activeRange[0] !== draftRange[0] || activeRange[1] !== draftRange[1]) return true;

    const active = [...this.activeFilterTypes()].sort();
    const pending = [...this.pendingFilterTypes()].sort();
    if (active.length !== pending.length) return true;
    return active.some((val, i) => val !== pending[i]);
  });

  ngOnInit(): void {
    this.refreshData();
  }

  refreshData(): void {
    if (!this.tradingAccountService.tradingAccount()) {
      this.tradingAccountService.loadTradingAccount();
    }
    this.walletService.loadWallet();
    this.loadLedger();
  }

  private loadLedger(): void {
    this.isLedgerLoading.set(true);
    this.ledgerService.getMyLedger().subscribe({
      next: (data) => {
        this.ledgerData.set(data);
        this.isLedgerLoading.set(false);
      },
      error: () => {
        this.ledgerData.set([]);
        this.isLedgerLoading.set(false);
      }
    });
  }

  requestMultiCurrencyAccess(): void {
    this.isRequestingAccess.set(true);
    this.walletService.requestMultiCurrency().subscribe({
      next: () => {
        this.toastService.success('Multi-currency request submitted successfully.');
        this.walletService.loadWallet();
        this.isRequestingAccess.set(false);
      },
      error: () => {
        this.isRequestingAccess.set(false);
      }
    });
  }

  formatLedgerType(type: LedgerEntryType): string {
    return type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
  }

  getLedgerBadgeColor(type: LedgerEntryType): BadgeColor {
    if (type.includes('CREDIT') || type === 'DEPOSIT' || type.includes('UNLOCK')) return 'success';
    if (type.includes('LOCK')) return 'warning';
    if (type.includes('DEBIT') || type.includes('FEE') || type === 'WITHDRAWAL') return 'danger';
    return 'primary';
  }

  formatLocaleNumber(value: number | undefined | null, currencyCode: string, style: 'currency' | 'decimal' = 'decimal', maxFraction = 2): string {
    if (value === null || value === undefined) return '0.00';
    const locale = currencyCode === 'INR' ? 'en-IN' : 'en-US';
    const minFrac = maxFraction === 0 ? 0 : 2;
    const maxFrac = Math.max(minFrac, maxFraction);
    return new Intl.NumberFormat(locale, {
      style: style,
      currency: style === 'currency' ? currencyCode : undefined,
      minimumFractionDigits: minFrac,
      maximumFractionDigits: maxFrac
    }).format(value);
  }

  openFilterDrawer(): void {
    this.isFilterDrawerOpen.set(true);
  }

  onAmountSliderChange(val: number | [number, number]): void {
    if (Array.isArray(val)) {
      this._draftAmountRange.set([val[0], val[1]]);
    }
  }

  applyFilters(): void {
    this.activeFilterTypes.set([...this.pendingFilterTypes()]);
    this.activeDirection.set(this.pendingDirection());
    this.activeStartDate.set(this.pendingStartDate());
    this.activeEndDate.set(this.pendingEndDate());

    const [min, max] = this.draftAmountRange();
    this._appliedAmountRange.set([Math.min(min, max), Math.max(min, max)]);

    this.isFilterDrawerOpen.set(false);
    this.tableCurrentPage.set(1);
  }

  resetFilters(): void {
    this.pendingFilterTypes.set([]);
    this.pendingDirection.set('ALL');
    this.pendingStartDate.set('');
    this.pendingEndDate.set('');
    this._draftAmountRange.set(null);
  }
}