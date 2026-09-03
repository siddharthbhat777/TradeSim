import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Card } from '../../../../shared/components/card/card';
import { Table, TableColumn } from '../../../../shared/components/table/table';
import { CustomInput } from '../../../../shared/components/input/input';
import { InputDirective } from '../../../../shared/directives/input';
import { PriceIndicator } from '../../../../shared/components/price-indicator/price-indicator';
import { PortfolioHoldingResponse } from '../../../../models/portfolio';

@Component({
  selector: 'app-holdings-table',
  imports: [CommonModule, FormsModule, Card, Table, CustomInput, InputDirective, PriceIndicator],
  templateUrl: './holdings-table.html',
  styleUrl: './holdings-table.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HoldingsTable {
  readonly holdings = input.required<PortfolioHoldingResponse[]>();
  readonly baseCurrency = input.required<string>();

  readonly searchQuery = signal<string>('');
  readonly currentPage = signal<number>(1);
  readonly pageSize = signal<number>(10);

  readonly filteredHoldings = computed(() => {
    const query = this.searchQuery().toLowerCase();
    if (!query) return this.holdings();
    return this.holdings().filter(h => h.symbol.toLowerCase().includes(query));
  });

  readonly columns: TableColumn<PortfolioHoldingResponse>[] = [
    { key: 'symbol', header: 'Symbol' },
    { key: 'quantity', header: 'Quantity', align: 'right' },
    { key: 'averageBuyPrice', header: 'Avg Price', align: 'right' },
    { key: 'currentPrice', header: 'Current Price', align: 'right' },
    { key: 'currentValue', header: 'Total Value', align: 'right' },
    { key: 'unrealizedPnl', header: 'Unrealized P&L', align: 'right' }
  ];
}