import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IpoService } from '../../../../services/ipo/ipo-service';
import { StockService } from '../../../../services/stock/stock-service';
import { TradingAccountService } from '../../../../services/trading-account/trading-account-service';
import { IpoOfferResponse } from '../../../../models/ipo';
import { Stock } from '../../../../models/stock';
import { Card } from '../../../../shared/components/card/card';
import { Table, TableCellDirective, TableColumn } from '../../../../shared/components/table/table';
import { CustomInput } from '../../../../shared/components/input/input';
import { InputDirective } from '../../../../shared/directives/input';
import { Dropdown, DropdownOption } from '../../../../shared/components/dropdown/dropdown';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';

export interface MappedUpcomingOffer extends IpoOfferResponse {
  symbol: string;
  sector: string;
  marketCapCategory: string;
}

@Component({
  selector: 'app-upcoming-ipos',
  imports: [
    CommonModule,
    FormsModule,
    Card,
    Table,
    TableCellDirective,
    CustomInput,
    InputDirective,
    Dropdown,
    EmptyState,
    CurrencyPipe,
    DatePipe
  ],
  templateUrl: './upcoming-ipos.html',
  styleUrl: './upcoming-ipos.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UpcomingIpos implements OnInit {
  private readonly ipoService = inject(IpoService);
  private readonly stockService = inject(StockService);
  private readonly tradingAccountService = inject(TradingAccountService);

  readonly baseCurrency = computed(() => this.tradingAccountService.tradingAccount()?.baseCurrency || 'INR');

  readonly rawOffers = signal<IpoOfferResponse[]>([]);
  readonly stocks = signal<Stock[]>([]);
  readonly isLoading = signal<boolean>(true);

  readonly searchQuery = signal<string>('');
  readonly selectedCategory = signal<string>('ALL');

  readonly categoryOptions: DropdownOption[] = [
    { label: 'All Categories', value: 'ALL' },
    { label: 'Large Cap', value: 'LARGE' },
    { label: 'Mid Cap', value: 'MID' },
    { label: 'Small Cap', value: 'SMALL' }
  ];

  readonly mappedOffers = computed<MappedUpcomingOffer[]>(() => {
    const stockData = this.stocks();
    const offers = this.rawOffers();

    return offers.map(offer => {
      const stock = stockData.find(s => s.id === offer.stockId);
      return {
        ...offer,
        symbol: stock?.symbol || 'UNKNOWN',
        sector: stock?.sector || 'UNKNOWN',
        marketCapCategory: stock?.marketCapCategory || 'UNKNOWN'
      };
    });
  });

  readonly processedOffers = computed<MappedUpcomingOffer[]>(() => {
    let filtered = [...this.mappedOffers()];

    const query = this.searchQuery().toLowerCase().trim();
    if (query) {
      filtered = filtered.filter(o => o.symbol.toLowerCase().includes(query));
    }

    if (this.selectedCategory() !== 'ALL') {
      filtered = filtered.filter(o => o.marketCapCategory === this.selectedCategory());
    }

    return filtered.sort((a, b) => new Date(a.subscriptionStartAt).getTime() - new Date(b.subscriptionStartAt).getTime());
  });

  readonly columns: TableColumn<MappedUpcomingOffer>[] = [
    { key: 'symbol', header: 'Symbol' },
    { key: 'marketCapCategory', header: 'Category' },
    { key: 'issuePrice', header: 'Issue Price', align: 'right' },
    { key: 'sharesPerAllottee', header: 'Lot Size', align: 'right' },
    { key: 'subscriptionStartAt', header: 'Opens On', align: 'right' },
    { key: 'subscriptionEndAt', header: 'Closes On', align: 'right' }
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
        this.ipoService.getUpcomingIpos().subscribe({
          next: (offers) => {
            this.rawOffers.set(offers);
            this.isLoading.set(false);
          },
          error: () => this.isLoading.set(false)
        });
      },
      error: () => this.isLoading.set(false)
    });
  }
}