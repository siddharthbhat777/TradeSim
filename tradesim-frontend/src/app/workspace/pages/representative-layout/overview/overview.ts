import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Card } from '../../../../shared/components/card/card';
import { Badge } from '../../../../shared/components/badge/badge';
import { Alert } from '../../../../shared/components/alert/alert';
import { PriceIndicator } from '../../../../shared/components/price-indicator/price-indicator';
import { AreaChart, AreaChartData } from '../../../../shared/components/charts/area-chart/area-chart';
import { Table, TableColumn, TableCellDirective } from '../../../../shared/components/table/table';
import { FormatCurrencyPipe } from '../../../../shared/pipes/format-currency-pipe';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { CustomInput } from '../../../../shared/components/input/input';
import { InputDirective } from '../../../../shared/directives/input';

import { CompanyService } from '../../../../services/company/company-service';
import { StockService } from '../../../../services/stock/stock-service';
import { ExchangeService } from '../../../../services/exchange/exchange-service';

import { CompanyResponse, CompanyRepresentativeAssignmentResponse } from '../../../../models/company';
import { Stock } from '../../../../models/stock';
import { Exchange } from '../../../../models/exchange';

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [
    CommonModule,
    Card,
    Badge,
    Alert,
    PriceIndicator,
    AreaChart,
    Table,
    TableCellDirective,
    FormatCurrencyPipe,
    CustomInput,
    InputDirective
  ],
  templateUrl: './overview.html',
  styleUrl: './overview.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Overview implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly companyService = inject(CompanyService);
  private readonly stockService = inject(StockService);
  private readonly exchangeService = inject(ExchangeService);
  private readonly toast = inject(ToastService);

  readonly isLoading = signal(true);
  readonly company = signal<CompanyResponse | null>(null);
  readonly stock = signal<Stock | null>(null);
  readonly exchange = signal<Exchange | null>(null);

  readonly allRepresentatives = signal<CompanyRepresentativeAssignmentResponse[]>([]);
  readonly searchQuery = signal('');

  readonly chartData = signal<AreaChartData[]>([]);

  readonly needsOnboardingAlert = computed(() => {
    const comp = this.company();
    const stk = this.stock();
    if (!comp) return false;
    return comp.status !== 'ACTIVE' || !stk || stk.status !== 'ACTIVE';
  });

  readonly filteredRepresentatives = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const reps = this.allRepresentatives();

    if (!query) return reps;

    return reps.filter(rep =>
      rep.userId.toLowerCase().includes(query) ||
      (rep.fullName && rep.fullName.toLowerCase().includes(query)) ||
      (rep.email && rep.email.toLowerCase().includes(query))
    );
  });

  readonly representativeColumns = signal<TableColumn<CompanyRepresentativeAssignmentResponse>[]>([
    { key: 'name', header: 'Representative Name' },
    { key: 'email', header: 'Email Address' },
    { key: 'userId', header: 'User ID' },
    { key: 'assignmentRole', header: 'Role' },
    { key: 'status', header: 'Status', align: 'right' }
  ]);

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const companyId = params.get('companyId') || this.route.parent?.snapshot.paramMap.get('companyId');

      if (companyId) {
        this.loadDashboard(companyId);
      }
    });
  }

  updateSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchQuery.set(input.value);
  }

  copyToClipboard(text: string, label: string): void {
    navigator.clipboard.writeText(text).then(() => {
      this.toast.success(`${label} copied to clipboard.`);
    }).catch(() => {
      this.toast.danger('Failed to copy to clipboard.');
    });
  }

  private loadDashboard(companyId: string): void {
    this.isLoading.set(true);

    this.companyService.getCompany(companyId).subscribe({
      next: (companyRes) => {
        this.company.set(companyRes);

        forkJoin({
          stocks: this.stockService.getStocks(),
          exchanges: this.exchangeService.getExchanges(),
          reps: this.companyService.getRepresentatives(companyId)
        }).subscribe({
          next: ({ stocks, exchanges, reps }) => {
            const matchedStock = stocks.find(s => s.symbol === companyRes.code) || null;
            this.stock.set(matchedStock);

            if (matchedStock) {
              const matchedExchange = exchanges.find(e => e.id === matchedStock.exchangeId) || null;
              this.exchange.set(matchedExchange);
              this.chartData.set(this.generateMockChartData(matchedStock.currentPrice));
            } else {
              this.exchange.set(null);
              this.chartData.set([]);
            }

            this.allRepresentatives.set(reps);
            this.isLoading.set(false);
          },
          error: () => this.isLoading.set(false)
        });
      },
      error: () => this.isLoading.set(false)
    });
  }

  private generateMockChartData(basePrice: number): AreaChartData[] {
    const data: AreaChartData[] = [];
    const now = new Date();
    let current = basePrice * 0.9;

    for (let i = 30; i >= 0; i--) {
      const time = new Date(now.getTime() - i * 24 * 60 * 60 * 1000);
      current = current + (Math.random() * basePrice * 0.04) - (basePrice * 0.018);
      data.push({ time, value: current });
    }

    data[data.length - 1].value = basePrice;
    return data;
  }
}