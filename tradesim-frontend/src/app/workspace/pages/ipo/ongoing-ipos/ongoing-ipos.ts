import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { IpoService } from '../../../../services/ipo/ipo-service';
import { StockService } from '../../../../services/stock/stock-service';
import { TradingAccountService } from '../../../../services/trading-account/trading-account-service';
import { DialogService } from '../../../../shared/components/dialog/dialog.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { IpoOfferResponse, IpoSubscriptionResponse } from '../../../../models/ipo';
import { Stock } from '../../../../models/stock';
import { Card } from '../../../../shared/components/card/card';
import { Table, TableCellDirective, TableColumn } from '../../../../shared/components/table/table';
import { CustomInput } from '../../../../shared/components/input/input';
import { InputDirective } from '../../../../shared/directives/input';
import { Dropdown, DropdownOption } from '../../../../shared/components/dropdown/dropdown';
import { Button } from '../../../../shared/components/button/button';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';

export interface MappedIpoOffer extends IpoOfferResponse {
  symbol: string;
  sector: string;
  marketCapCategory: string;
  hasApplied: boolean;
}

@Component({
  selector: 'app-ongoing-ipos',
  imports: [
    CommonModule,
    FormsModule,
    Card,
    Table,
    TableCellDirective,
    CustomInput,
    InputDirective,
    Dropdown,
    Button,
    EmptyState,
    CurrencyPipe,
    DatePipe
  ],
  templateUrl: './ongoing-ipos.html',
  styleUrl: './ongoing-ipos.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OngoingIpos implements OnInit {
  private readonly ipoService = inject(IpoService);
  private readonly stockService = inject(StockService);
  private readonly tradingAccountService = inject(TradingAccountService);
  private readonly dialogService = inject(DialogService);
  private readonly toastService = inject(ToastService);

  readonly baseCurrency = computed(() => this.tradingAccountService.tradingAccount()?.baseCurrency || 'INR');

  readonly rawOffers = signal<IpoOfferResponse[]>([]);
  readonly userSubscriptions = signal<IpoSubscriptionResponse[]>([]);
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

  readonly mappedOffers = computed<MappedIpoOffer[]>(() => {
    const stockData = this.stocks();
    const offers = this.rawOffers();
    const subs = this.userSubscriptions();

    return offers.map(offer => {
      const stock = stockData.find(s => s.id === offer.stockId);
      const hasApplied = subs.some(sub => sub.ipoOfferId === offer.id);

      return {
        ...offer,
        symbol: stock?.symbol || 'UNKNOWN',
        sector: stock?.sector || 'UNKNOWN',
        marketCapCategory: stock?.marketCapCategory || 'UNKNOWN',
        hasApplied
      };
    });
  });

  readonly processedOffers = computed<MappedIpoOffer[]>(() => {
    let filtered = [...this.mappedOffers()];

    const query = this.searchQuery().toLowerCase().trim();
    if (query) {
      filtered = filtered.filter(o => o.symbol.toLowerCase().includes(query));
    }

    if (this.selectedCategory() !== 'ALL') {
      filtered = filtered.filter(o => o.marketCapCategory === this.selectedCategory());
    }

    return filtered.sort((a, b) => new Date(a.subscriptionEndAt).getTime() - new Date(b.subscriptionEndAt).getTime());
  });

  readonly columns: TableColumn<MappedIpoOffer>[] = [
    { key: 'symbol', header: 'Symbol' },
    { key: 'marketCapCategory', header: 'Category' },
    { key: 'issuePrice', header: 'Issue Price', align: 'right' },
    { key: 'sharesPerAllottee', header: 'Lot Size', align: 'right' },
    { key: 'subscriptionEndAt', header: 'Closes On', align: 'right' },
    { key: 'actions', header: '', align: 'right' }
  ];

  readonly currentPage = signal<number>(1);
  readonly pageSize = signal<number>(10);
  readonly isSubmitting = signal<string | null>(null);

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

        forkJoin({
          offers: this.ipoService.getOpenIpos(),
          subscriptions: this.ipoService.getMySubscriptions()
        }).subscribe({
          next: (res) => {
            this.rawOffers.set(res.offers);
            this.userSubscriptions.set(res.subscriptions);
            this.isLoading.set(false);
          },
          error: () => this.isLoading.set(false)
        });
      },
      error: () => this.isLoading.set(false)
    });
  }

  subscribe(offer: MappedIpoOffer): void {
    const totalCost = offer.issuePrice * offer.sharesPerAllottee;
    const formattedCost = `${totalCost.toFixed(2)} ${this.baseCurrency()}`;

    const formattedHtml = `
      <div style="display: flex; flex-direction: column; gap: 16px;">
        <span style="font-size: 0.95rem; color: var(--text-primary);">
          You are applying for the <strong style="color: var(--primary);">${offer.symbol}</strong> IPO.
        </span>
        <div style="background: var(--surface-secondary); padding: 16px; border-radius: 8px; border: 1px solid var(--border);">
          <span style="display: block; font-size: 0.85rem; color: var(--text-muted); margin-bottom: 4px;">
            Amount to be locked
          </span>
          <strong style="display: block; font-size: 1.75rem; font-weight: 900; color: var(--success); letter-spacing: -0.5px;">
            ${formattedCost}
          </strong>
        </div>
        <span style="font-size: 0.9rem; color: var(--text-secondary); line-height: 1.5;">
          These funds will remain securely locked in your wallet until the allotment is finalized.
        </span>
      </div>
    `;

    this.dialogService.open({
      title: 'Confirm IPO Subscription',
      messageHtml: formattedHtml,
      primaryLabel: 'Confirm Subscription',
      secondaryLabel: 'Cancel',
      primaryVariant: 'primary',
      onPrimary: () => {
        this.isSubmitting.set(offer.id);
        this.ipoService.subscribeToIpo(offer.id).subscribe({
          next: () => {
            this.toastService.success(`Successfully applied for the ${offer.symbol} IPO.`);
            this.isSubmitting.set(null);
            this.loadData();
          },
          error: () => {
            this.isSubmitting.set(null);
          }
        });
      }
    });
  }
}