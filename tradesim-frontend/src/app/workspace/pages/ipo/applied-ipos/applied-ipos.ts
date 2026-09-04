import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IpoService } from '../../../../services/ipo/ipo-service';
import { StockService } from '../../../../services/stock/stock-service';
import { TradingAccountService } from '../../../../services/trading-account/trading-account-service';
import { IpoSubscriptionResponse } from '../../../../models/ipo';
import { Stock } from '../../../../models/stock';
import { Card } from '../../../../shared/components/card/card';
import { Table, TableCellDirective, TableColumn } from '../../../../shared/components/table/table';
import { CustomInput } from '../../../../shared/components/input/input';
import { InputDirective } from '../../../../shared/directives/input';
import { Dropdown, DropdownOption } from '../../../../shared/components/dropdown/dropdown';
import { Badge } from '../../../../shared/components/badge/badge';

export interface MappedIpoSubscription extends IpoSubscriptionResponse {
  symbol: string;
}

@Component({
  selector: 'app-applied-ipos',
  imports: [
    CommonModule,
    FormsModule,
    Card,
    Table,
    TableCellDirective,
    CustomInput,
    InputDirective,
    Dropdown,
    Badge,
    CurrencyPipe,
    DatePipe
  ],
  templateUrl: './applied-ipos.html',
  styleUrl: './applied-ipos.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppliedIpos implements OnInit {
  private readonly ipoService = inject(IpoService);
  private readonly stockService = inject(StockService);
  private readonly tradingAccountService = inject(TradingAccountService);

  readonly baseCurrency = computed(() => this.tradingAccountService.tradingAccount()?.baseCurrency || 'INR');

  readonly rawSubscriptions = signal<IpoSubscriptionResponse[]>([]);
  readonly stocks = signal<Stock[]>([]);
  readonly isLoading = signal<boolean>(true);

  readonly searchQuery = signal<string>('');
  readonly selectedStatus = signal<string>('ALL');

  readonly statusOptions: DropdownOption[] = [
    { label: 'All Statuses', value: 'ALL' },
    { label: 'Submitted', value: 'SUBMITTED' },
    { label: 'Allotted', value: 'ALLOTTED' },
    { label: 'Not Allotted', value: 'NOT_ALLOTTED' }
  ];

  readonly mappedSubscriptions = computed<MappedIpoSubscription[]>(() => {
    const stockData = this.stocks();
    const subscriptions = this.rawSubscriptions();

    return subscriptions.map(sub => {
      const stock = stockData.find(s => s.id === sub.stockId);
      return {
        ...sub,
        symbol: stock?.symbol || 'UNKNOWN'
      };
    });
  });

  readonly processedSubscriptions = computed<MappedIpoSubscription[]>(() => {
    let filtered = [...this.mappedSubscriptions()];

    const query = this.searchQuery().toLowerCase().trim();
    if (query) {
      filtered = filtered.filter(s => s.symbol.toLowerCase().includes(query));
    }

    if (this.selectedStatus() !== 'ALL') {
      filtered = filtered.filter(s => s.status === this.selectedStatus());
    }

    return filtered;
  });

  readonly columns: TableColumn<MappedIpoSubscription>[] = [
    { key: 'symbol', header: 'Symbol' },
    { key: 'issuePrice', header: 'Issue Price', align: 'right' },
    { key: 'lockedAmount', header: 'Locked Amount', align: 'right' },
    { key: 'allottedShares', header: 'Allotted Shares', align: 'right' },
    { key: 'status', header: 'Status', align: 'center' },
    { key: 'createdAt', header: 'Applied On', align: 'right' }
  ];

  readonly currentPage = signal<number>(1);
  readonly pageSize = signal<number>(10);

  ngOnInit(): void {
    if (!this.tradingAccountService.tradingAccount()) {
      this.tradingAccountService.loadTradingAccount();
    }
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.stockService.getStocks().subscribe({
      next: (stocks) => {
        this.stocks.set(stocks);
        this.ipoService.getMySubscriptions().subscribe({
          next: (subscriptions) => {
            this.rawSubscriptions.set(subscriptions);
            this.isLoading.set(false);
          },
          error: () => this.isLoading.set(false)
        });
      },
      error: () => this.isLoading.set(false)
    });
  }
}