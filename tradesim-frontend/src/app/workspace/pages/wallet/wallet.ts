import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
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

@Component({
  selector: 'app-wallet',
  imports: [
    CommonModule,
    FormsModule,
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
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
    FundManager
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

  readonly filterTypeOptions = [
    { label: 'Deposits & Withdrawals', value: 'TRANSFER' },
    { label: 'Trading & Margin', value: 'TRADE' },
    { label: 'IPO Actions', value: 'IPO' },
    { label: 'Fees', value: 'FEE' }
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

    if (query) {
      data = data.filter(entry =>
        entry.description.toLowerCase().includes(query) ||
        entry.currency.toLowerCase().includes(query)
      );
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

  readonly hasFilterChanges = computed(() => {
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

  openFilterDrawer(): void {
    this.isFilterDrawerOpen.set(true);
  }

  applyFilters(): void {
    this.activeFilterTypes.set([...this.pendingFilterTypes()]);
    this.isFilterDrawerOpen.set(false);
    this.tableCurrentPage.set(1);
  }

  resetFilters(): void {
    this.pendingFilterTypes.set([]);
  }
}