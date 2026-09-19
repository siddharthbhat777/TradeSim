import { ChangeDetectionStrategy, Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { Card } from '../../../../shared/components/card/card';
import { Badge } from '../../../../shared/components/badge/badge';
import { Table, TableColumn, TableCellDirective } from '../../../../shared/components/table/table';
import { Dropdown, DropdownOption } from '../../../../shared/components/dropdown/dropdown';
import { CandlestickChart, CandlestickData } from '../../../../shared/components/charts/candlestick-chart/candlestick-chart';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { Button } from '../../../../shared/components/button/button';

import { ExchangeService } from '../../../../services/exchange/exchange-service';
import { MarketIndexService } from '../../../../services/market-index/market-index-service';
import { UserService } from '../../../../services/user/user-service';
import { CompanyService } from '../../../../services/company/company-service';
import { IpoService } from '../../../../services/ipo/ipo-service';
import { ListingService } from '../../../../services/listing/listing-service';
import { WalletService } from '../../../../services/wallet/wallet-service';

import { Exchange } from '../../../../models/exchange';
import { MarketIndex } from '../../../../models/market-index';

interface DashboardStats {
  totalUsers: number;
  activeCompanies: number;
  pendingApprovals: number;
  activeExchanges: number;
}

interface PendingAction {
  id: string;
  type: 'IPO' | 'Listing' | 'Wallet';
  entityName: string;
  submittedAt: string;
}

@Component({
  selector: 'app-dashboard',
  imports: [
    CommonModule,
    FormsModule,
    Card,
    Badge,
    Table,
    TableCellDirective,
    Dropdown,
    CandlestickChart,
    EmptyState,
    Button
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Dashboard implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly exchangeService = inject(ExchangeService);
  private readonly marketIndexService = inject(MarketIndexService);
  private readonly userService = inject(UserService);
  private readonly companyService = inject(CompanyService);
  private readonly ipoService = inject(IpoService);
  private readonly listingService = inject(ListingService);
  private readonly walletService = inject(WalletService);

  readonly stats = signal<DashboardStats | null>(null);

  readonly exchanges = signal<Exchange[]>([]);
  readonly exchangeOptions = computed<DropdownOption<string>[]>(() =>
    this.exchanges().map(e => ({ label: e.code, value: e.id, fullName: e.name }))
  );
  readonly selectedExchangeId = signal<string | null>(null);

  readonly indices = signal<MarketIndex[]>([]);
  readonly indexOptions = computed<DropdownOption<string>[]>(() =>
    this.indices().map(i => ({ label: i.name, value: i.id }))
  );
  readonly selectedIndexId = signal<string | null>(null);

  readonly chartData = signal<CandlestickData[]>([]);

  readonly pendingActions = signal<PendingAction[]>([]);
  readonly actionColumns = signal<TableColumn<PendingAction>[]>([
    { key: 'type', header: 'Request Type', width: '140px' },
    { key: 'entityName', header: 'Entity / Subject' },
    { key: 'submittedAt', header: 'Date Submitted' },
    { key: 'id', header: '', align: 'right', width: '60px' }
  ]);

  constructor() {
    effect(() => {
      const exchangeId = this.selectedExchangeId();
      if (exchangeId) {
        this.loadIndices(exchangeId);
      } else {
        this.indices.set([]);
        this.selectedIndexId.set(null);
      }
    }, { allowSignalWrites: true });

    effect(() => {
      const indexId = this.selectedIndexId();
      const currentIndices = this.indices();

      if (indexId && currentIndices.length > 0) {
        const selected = currentIndices.find(i => i.id === indexId);
        if (selected && selected.dayOpen != null && selected.currentValue != null) {
          this.chartData.set([{
            time: new Date(),
            open: selected.dayOpen,
            high: selected.dayHigh ?? selected.dayOpen,
            low: selected.dayLow ?? selected.dayOpen,
            close: selected.currentValue
          }]);
        } else {
          this.chartData.set([]);
        }
      } else {
        this.chartData.set([]);
      }
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void {
    this.loadDashboardData();
  }

  onExchangeChange(val: string | null): void {
    if (val) {
      this.selectedExchangeId.set(val);
    } else {
      const current = this.selectedExchangeId();
      this.selectedExchangeId.set(null);
      setTimeout(() => this.selectedExchangeId.set(current));
    }
  }

  onIndexChange(val: string | null): void {
    if (val) {
      this.selectedIndexId.set(val);
    } else {
      const current = this.selectedIndexId();
      this.selectedIndexId.set(null);
      setTimeout(() => this.selectedIndexId.set(current));
    }
  }

  onActionClick(row: PendingAction): void {
    this.router.navigate(['../approvals'], {
      relativeTo: this.route,
      queryParams: { type: row.type }
    });
  }

  goToApprovals(): void {
    this.router.navigate(['../approvals'], {
      relativeTo: this.route
    });
  }

  getBadgeColor(type: string): 'accent' | 'secondary' | 'primary' | 'success' | 'warning' | 'danger' {
    switch (type) {
      case 'IPO': return 'accent';
      case 'Listing': return 'success';
      case 'Wallet': return 'primary';
      default: return 'primary';
    }
  }

  private loadDashboardData(): void {
    forkJoin({
      users: this.userService.getAllUsers().pipe(catchError(() => of([]))),
      companies: this.companyService.getCompanies().pipe(catchError(() => of([]))),
      exchanges: this.exchangeService.getExchanges().pipe(catchError(() => of([]))),
      pendingIpos: this.ipoService.getPendingIpos().pipe(catchError(() => of([]))),
      pendingListings: this.listingService.getPendingExchangeRequests().pipe(catchError(() => of([]))),
      pendingWallets: this.walletService.getPendingMultiCurrencyRequests().pipe(catchError(() => of([])))
    }).subscribe({
      next: ({ users, companies, exchanges, pendingIpos, pendingListings, pendingWallets }) => {
        this.exchanges.set(exchanges);
        if (exchanges.length > 0) {
          this.selectedExchangeId.set(exchanges[0].id);
        }

        const nonAdminUsers = users.filter(u => u.role !== 'ADMIN');

        this.stats.set({
          totalUsers: nonAdminUsers.length,
          activeCompanies: companies.filter(c => c.status === 'ACTIVE').length,
          activeExchanges: exchanges.filter(e => e.status === 'ACTIVE').length,
          pendingApprovals: pendingIpos.length + pendingListings.length + pendingWallets.length
        });

        const now = new Date().toISOString();
        const actions: PendingAction[] = [
          ...pendingListings.map(l => ({
            id: l.id,
            type: 'Listing' as const,
            entityName: l.symbol,
            submittedAt: l.createdAt
          })),
          ...pendingIpos.map(i => ({
            id: i.id,
            type: 'IPO' as const,
            entityName: i.symbol,
            submittedAt: i.createdAt
          })),
          ...pendingWallets.map(w => ({
            id: w.id,
            type: 'Wallet' as const,
            entityName: `User ID: ${w.userId}`,
            submittedAt: w.createdAt || now
          }))
        ];

        actions.sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime());
        this.pendingActions.set(actions.slice(0, 5));
      }
    });
  }

  private loadIndices(exchangeId: string): void {
    this.marketIndexService.getIndicesByExchange(exchangeId).subscribe({
      next: (indices) => {
        this.indices.set(indices);
        if (indices.length > 0) {
          this.selectedIndexId.set(indices[0].id);
        } else {
          this.selectedIndexId.set(null);
        }
      }
    });
  }
}